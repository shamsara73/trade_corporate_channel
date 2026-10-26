package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.txn.ChannelRefs;
import com.bank.tradechannel.domain.txn.Customer;
import com.bank.tradechannel.domain.txn.DuplicateReferenceException;
import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.OutboxEntry;
import com.bank.tradechannel.domain.txn.OutboxEntry.OutboxStatus;
import com.bank.tradechannel.domain.txn.OutboxStore;
import com.bank.tradechannel.domain.txn.TiDocument;
import com.bank.tradechannel.domain.txn.TradeEvent;
import com.bank.tradechannel.domain.txn.TradeTxn;
import com.bank.tradechannel.domain.txn.TxnStatus;
import com.bank.tradechannel.domain.txn.TxnStore;
import com.bank.tradechannel.domain.txn.UnitOfWork;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The Finance Standalone (FSA) workflow: start a finance (CREATE event), repay it (REPAY events),
 * and show where each stands. Every new event is written together with its outbox message in one
 * database transaction (ground rule 7); the outbox dispatcher delivers it to TI.
 */
public final class FsaService {
  public static final String PRODUCT_CODE = "FSA";

  private final TxnStore txns;
  private final FsaDetailStore details;
  private final OutboxStore outbox;
  private final UnitOfWork uow;
  private final ReferenceGenerator references;
  private final Clock clock;

  public FsaService(
      TxnStore txns,
      FsaDetailStore details,
      OutboxStore outbox,
      UnitOfWork uow,
      ReferenceGenerator references,
      Clock clock) {
    this.txns = txns;
    this.details = details;
    this.outbox = outbox;
    this.uow = uow;
    this.references = references;
    this.clock = clock;
  }

  /** Starts a new finance: the transaction, its CREATE event and the TFFINNEW message. */
  public TradeTxn create(Customer customer, FinanceRequest request, String user) {
    return withFreshReferences(() -> tryCreate(customer, request, user));
  }

  private TradeTxn tryCreate(Customer customer, FinanceRequest request, String user) {
    Instant now = clock.instant();
    ChannelRefs refs = references.newTransaction();
    var txn =
        new TradeTxn(
            UUID.randomUUID(), customer, PRODUCT_CODE, refs.eBankMasterRef(),
            request.theirReference(), TxnStatus.PENDING,
            request.financeAmount().currencyCode(), request.financeAmount().amount(),
            Optional.empty(), request.startDate(), request.maturityDate(), Optional.empty(),
            Optional.empty(), user, now, now, 0);
    var event = newEvent(txn.id(), EventType.CREATE, 1, refs, user, now);
    return uow.inTransaction(
        () -> {
          txns.insert(txn);
          txns.insert(event);
          details.saveCreate(event.id(), request);
          outbox.insert(newEntry(event, refs, now));
          return txn;
        });
  }

  /**
   * Starts a REPAY event on a live finance. Throws {@link RepaymentRefusedException} if a repayment
   * may not start now (FSA-1, FSA-2) or the request breaks a rule (FSA-3, FSA-4).
   */
  public TradeEvent repay(Customer customer, UUID txnId, RepaymentRequest request, String user) {
    return withFreshReferences(() -> tryRepay(customer, txnId, request, user));
  }

  private TradeEvent tryRepay(Customer customer, UUID txnId, RepaymentRequest request, String user) {
    return uow.inTransaction(
        () -> {
          TradeTxn txn =
              txns.find(customer.mnemonic(), txnId)
                  .orElseThrow(() -> new IllegalArgumentException("no such transaction"));
          List<TradeEvent> events = txns.events(txnId);
          Optional<String> notNow = FsaRules.whyNoRepay(txn, events);
          if (notNow.isPresent()) {
            throw new RepaymentRefusedException(notNow, Map.of());
          }
          Map<String, String> errors = FsaRules.checkRepayment(txn, request);
          if (!errors.isEmpty()) {
            throw new RepaymentRefusedException(Optional.empty(), errors);
          }
          Instant now = clock.instant();
          // Touching the transaction takes its version: two simultaneous repayments cannot both pass
          // the one-in-flight rule.
          txns.update(txn.withStatus(txn.status(), now));
          int seq = events.stream().mapToInt(TradeEvent::seq).max().orElse(0) + 1;
          ChannelRefs refs = references.newEvent(txn.eBankMasterRef());
          var event = newEvent(txnId, EventType.REPAY, seq, refs, user, now);
          txns.insert(event);
          details.saveRepay(event.id(), request);
          outbox.insert(newEntry(event, refs, now));
          return event;
        });
  }

  public List<TradeTxn> list(Customer customer) {
    return txns.list(customer.mnemonic());
  }

  /** Everything the transaction page shows, if the transaction belongs to the customer. */
  public Optional<TxnView> view(Customer customer, UUID txnId) {
    return txns.find(customer.mnemonic(), txnId)
        .map(
            txn -> {
              List<TradeEvent> events = txns.events(txnId);
              List<EventView> views =
                  events.stream()
                      .sorted(Comparator.comparingInt(TradeEvent::seq))
                      .map(
                          e ->
                              new EventView(
                                  e,
                                  e.type() == EventType.CREATE ? details.create(e.id()) : Optional.empty(),
                                  e.type() == EventType.REPAY ? details.repay(e.id()) : Optional.empty(),
                                  outbox.forEvent(e.id()).stream()
                                      .max(Comparator.comparing(OutboxEntry::createdAt))))
                      .toList();
              return new TxnView(txn, views, txns.documents(txnId), FsaRules.whyNoRepay(txn, events));
            });
  }

  /** A document TI sent for one of the customer's transactions. */
  public Optional<TxnStore.DocumentContent> document(Customer customer, UUID txnId, UUID documentId) {
    return txns.documentContent(customer.mnemonic(), txnId, documentId);
  }

  /** The database rolled back the attempt; a new attempt draws new references. */
  private static <T> T withFreshReferences(Supplier<T> attempt) {
    for (int tries = 1; ; tries++) {
      try {
        return attempt.get();
      } catch (DuplicateReferenceException e) {
        if (tries == 5) {
          throw e;
        }
      }
    }
  }

  private static TradeEvent newEvent(
      UUID txnId, EventType type, int seq, ChannelRefs refs, String user, Instant now) {
    return new TradeEvent(
        UUID.randomUUID(), txnId, type, seq, refs.eBankEventRef(), EventStatus.PENDING,
        Optional.empty(), Optional.empty(), Optional.empty(), user, now, now, Optional.empty(), 0);
  }

  private static OutboxEntry newEntry(TradeEvent event, ChannelRefs refs, Instant now) {
    return new OutboxEntry(
        UUID.randomUUID(), event.id(), refs.theirRef(), event.type().operation(),
        OutboxStatus.PENDING, 0, now, Optional.empty(), Optional.empty(), Optional.empty(),
        Optional.empty(), Optional.empty(), Optional.empty(), now, 0);
  }

  /** The transaction page: the transaction, its events oldest first, TI's documents. */
  public record TxnView(
      TradeTxn txn, List<EventView> events, List<TiDocument> documents, Optional<String> whyNoRepay) {

    public boolean canRepay() {
      return whyNoRepay.isEmpty();
    }

    /** Something is with TI: the page refreshes itself until it is decided. */
    public boolean waiting() {
      return events.stream().anyMatch(e -> e.event().status().inFlight());
    }
  }

  /** One event with what the customer asked for and its latest delivery. */
  public record EventView(
      TradeEvent event,
      Optional<FinanceRequest> create,
      Optional<RepaymentRequest> repay,
      Optional<OutboxEntry> delivery) {}
}
