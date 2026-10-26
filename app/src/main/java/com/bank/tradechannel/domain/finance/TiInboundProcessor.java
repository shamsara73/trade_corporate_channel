package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.InboundStore;
import com.bank.tradechannel.domain.txn.InboundStore.InboundRecord;
import com.bank.tradechannel.domain.txn.InboundStore.Outcome;
import com.bank.tradechannel.domain.txn.OutboxEntry;
import com.bank.tradechannel.domain.txn.OutboxStore;
import com.bank.tradechannel.domain.txn.TiDocument;
import com.bank.tradechannel.domain.txn.TiInbound;
import com.bank.tradechannel.domain.txn.TradeEvent;
import com.bank.tradechannel.domain.txn.TradeTxn;
import com.bank.tradechannel.domain.txn.TxnStatus;
import com.bank.tradechannel.domain.txn.TxnStore;
import com.bank.tradechannel.domain.txn.UnitOfWork;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies TI's messages to the transactions: the immediate ServiceResponse moves an event to RECEIVED
 * (or FAILED); an outward ACK or REJ records the back office's decision and, for an ACK, what TI
 * booked. Every message is kept, matched or not; processing the same message twice changes nothing.
 */
public final class TiInboundProcessor {
  private final TxnStore txns;
  private final OutboxStore outbox;
  private final FsaDetailStore details;
  private final InboundStore inbound;
  private final UnitOfWork uow;
  private final Clock clock;

  public TiInboundProcessor(
      TxnStore txns,
      OutboxStore outbox,
      FsaDetailStore details,
      InboundStore inbound,
      UnitOfWork uow,
      Clock clock) {
    this.txns = txns;
    this.outbox = outbox;
    this.details = details;
    this.inbound = inbound;
    this.uow = uow;
    this.clock = clock;
  }

  /** Handles one message from {@code source} (a queue or a directory) and says what happened. */
  public Outcome handle(TiInbound message, String source) {
    return uow.inTransaction(
        () -> {
          if (message instanceof TiInbound.Reply reply) {
            return reply(reply, source);
          }
          if (message instanceof TiInbound.Outward outward) {
            return outward(outward, source);
          }
          var unreadable = (TiInbound.Unreadable) message;
          record(source, "UNREADABLE", Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty(), Optional.empty(), Outcome.UNREADABLE, unreadable.raw());
          return Outcome.UNREADABLE;
        });
  }

  private Outcome reply(TiInbound.Reply reply, String source) {
    Optional<OutboxEntry> entry = outbox.findByTheirRef(reply.correlationId());
    Optional<TradeEvent> event = entry.flatMap(e -> txns.event(e.eventId()));
    Outcome outcome = Outcome.UNMATCHED;
    if (entry.isPresent() && event.isPresent()) {
      Instant now = clock.instant();
      String detailText = String.join("; ", reply.details());
      var e = entry.get();
      outbox.update(
          new OutboxEntry(e.id(), e.eventId(), e.theirRef(), e.operation(), e.status(), e.attempts(),
              e.nextAttemptAt(), e.sentAt(), e.destination(), e.lastError(),
              Optional.of(reply.status()), Optional.of(detailText).filter(t -> !t.isEmpty()),
              Optional.of(now), e.createdAt(), e.version()),
          Optional.empty());
      EventStatus next = reply.succeeded() ? EventStatus.RECEIVED : EventStatus.FAILED;
      Optional<String> error =
          reply.succeeded() ? Optional.empty() : Optional.of("TI: " + reply.status() + " " + detailText);
      outcome = moveEvent(event.get(), next, Optional.empty(), Optional.empty(), error, now)
          ? Outcome.MATCHED
          : Outcome.DUPLICATE;
      if (outcome == Outcome.MATCHED && next == EventStatus.FAILED
          && event.get().type() == EventType.CREATE) {
        TradeTxn txn = txns.findById(event.get().txnId()).orElseThrow();
        txns.update(txn.withStatus(TxnStatus.FAILED, now));
      }
    }
    record(source, "REPLY", Optional.of(reply.operation()), Optional.empty(),
        Optional.of(reply.correlationId()), Optional.empty(), Optional.empty(),
        event.map(TradeEvent::id), outcome, reply.raw());
    return outcome;
  }

  private Outcome outward(TiInbound.Outward m, String source) {
    Optional<TradeEvent> found =
        m.eBankMasterRef().isPresent() && m.eBankEventRef().isPresent()
            ? txns.eventByRefs(m.eBankMasterRef().get(), m.eBankEventRef().get())
            : Optional.empty();
    Outcome outcome = Outcome.UNMATCHED;
    if (found.isPresent() && (m.kind() == TiInbound.Kind.ACK || m.kind() == TiInbound.Kind.REJ)) {
      Instant now = clock.instant();
      TradeEvent event = found.get();
      boolean ack = m.kind() == TiInbound.Kind.ACK;
      boolean moved =
          moveEvent(event, ack ? EventStatus.COMPLETED : EventStatus.REJECTED, m.tiEventRef(),
              m.bankComment(), Optional.empty(), now);
      if (moved) {
        applyToTransaction(event, m, ack, now);
        m.attachments().forEach(a -> txns.insert(
            new TiDocument(UUID.randomUUID(), event.id(), a.documentId(), a.description(),
                a.mimeType(), a.reference(), now),
            a.content()));
      }
      outcome = moved ? Outcome.MATCHED : Outcome.DUPLICATE;
    }
    record(source, m.kind().name(), Optional.of(m.operation()), Optional.of(m.messageName()),
        Optional.empty(), m.eBankMasterRef(), m.eBankEventRef(), found.map(TradeEvent::id), outcome,
        m.raw());
    return outcome;
  }

  /**
   * CREATE: an ACK means TI booked the finance (LIVE, with TI's master reference, booked amount,
   * outstanding and maturity); a REJ means it never existed (REJECTED). REPAY: an ACK lowers what is
   * outstanding, to TI's figure if it sent one (REPAID at zero); a REJ changes nothing.
   */
  private void applyToTransaction(TradeEvent event, TiInbound.Outward m, boolean ack, Instant now) {
    TradeTxn txn = txns.findById(event.txnId()).orElseThrow();
    if (event.type() == EventType.CREATE) {
      if (!ack) {
        txns.update(txn.withStatus(TxnStatus.REJECTED, now));
        return;
      }
      BigDecimal booked = major(m.amountMinor(), m.amountCurrency(), txn).orElse(txn.financeAmount());
      BigDecimal outstanding = major(m.outstandingMinor(), m.outstandingCurrency(), txn).orElse(booked);
      txns.update(
          txn.bookedByTi(statusFor(outstanding), booked, Optional.of(outstanding),
              m.maturityDate().orElse(txn.maturityDate()), m.tiMasterRef(), m.masterStatus(), now));
      return;
    }
    if (!ack) {
      return;
    }
    BigDecimal repaid =
        details.repay(event.id()).map(r -> r.principal().amount()).orElse(BigDecimal.ZERO);
    BigDecimal outstanding =
        major(m.outstandingMinor(), m.outstandingCurrency(), txn)
            .orElse(txn.outstanding().orElse(BigDecimal.ZERO).subtract(repaid).max(BigDecimal.ZERO));
    txns.update(
        txn.bookedByTi(statusFor(outstanding), txn.financeAmount(), Optional.of(outstanding),
            m.maturityDate().orElse(txn.maturityDate()), m.tiMasterRef(), m.masterStatus(), now));
  }

  private static TxnStatus statusFor(BigDecimal outstanding) {
    return outstanding.signum() > 0 ? TxnStatus.LIVE : TxnStatus.REPAID;
  }

  /**
   * TI sends amounts in minor units ({@code 10200} for USD 102.00). ASSUMPTION(Q35): the scale is
   * the ISO 4217 one of the accompanying currency. A value with a decimal point is taken as is.
   */
  static Optional<BigDecimal> major(Optional<String> minor, Optional<String> currency, TradeTxn txn) {
    return minor.map(
        v -> {
          BigDecimal value = new BigDecimal(v.trim());
          if (v.contains(".")) {
            return value;
          }
          int digits = Currency.getInstance(currency.orElse(txn.currencyCode())).getDefaultFractionDigits();
          return value.movePointLeft(Math.max(digits, 0)).setScale(Math.max(digits, 0));
        });
  }

  private boolean moveEvent(
      TradeEvent event,
      EventStatus next,
      Optional<String> tiEventRef,
      Optional<String> comment,
      Optional<String> error,
      Instant now) {
    if (event.status() == next || !event.status().canMoveTo(next)) {
      return false;
    }
    txns.update(event.moveTo(next, tiEventRef, comment, error, now));
    return true;
  }

  private void record(
      String source,
      String kind,
      Optional<String> operation,
      Optional<String> messageName,
      Optional<String> correlationId,
      Optional<String> master,
      Optional<String> event,
      Optional<UUID> eventId,
      Outcome outcome,
      String raw) {
    inbound.record(
        new InboundRecord(UUID.randomUUID(), clock.instant(), source, kind, operation, messageName,
            correlationId, master, event, eventId, outcome, raw));
  }
}
