package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.finance.TradeGateway.GatewayUnavailableException;
import com.bank.tradechannel.domain.txn.ChannelRefs;
import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.OutboxEntry;
import com.bank.tradechannel.domain.txn.OutboxEntry.OutboxStatus;
import com.bank.tradechannel.domain.txn.OutboxStore;
import com.bank.tradechannel.domain.txn.TradeEvent;
import com.bank.tradechannel.domain.txn.TradeTxn;
import com.bank.tradechannel.domain.txn.TxnStatus;
import com.bank.tradechannel.domain.txn.TxnStore;
import com.bank.tradechannel.domain.txn.UnitOfWork;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Delivers due outbox messages to TI. A delivery that fails (TI's queue unreachable: nothing was
 * delivered) is retried with growing delays and the same references; after the last attempt the
 * event fails.
 */
public final class OutboxDispatcher {
  static final int MAX_ATTEMPTS = 5;
  static final Duration LEASE = Duration.ofMinutes(2);

  private final OutboxStore outbox;
  private final TxnStore txns;
  private final FsaDetailStore details;
  private final TradeGateway gateway;
  private final UnitOfWork uow;
  private final Clock clock;

  public OutboxDispatcher(
      OutboxStore outbox,
      TxnStore txns,
      FsaDetailStore details,
      TradeGateway gateway,
      UnitOfWork uow,
      Clock clock) {
    this.outbox = outbox;
    this.txns = txns;
    this.details = details;
    this.gateway = gateway;
    this.uow = uow;
    this.clock = clock;
  }

  /** Delivers what is due; returns how many messages it tried. */
  public int dispatchDue() {
    Instant now = clock.instant();
    List<OutboxEntry> claimed = uow.inTransaction(() -> outbox.claimDue(now, now.minus(LEASE), 10));
    claimed.forEach(this::deliver);
    return claimed.size();
  }

  private void deliver(OutboxEntry entry) {
    TradeGateway.SendReceipt receipt;
    try {
      OutboundMessage message = uow.inTransaction(() -> assemble(entry));
      receipt = gateway.send(message);
    } catch (GatewayUnavailableException e) {
      uow.inTransaction(() -> markRetryOrFailed(entry, e.getMessage()));
      return;
    } catch (RuntimeException e) {
      // Not a transport problem (for example a message that cannot be written): still counts as an
      // attempt, so it ends as FAILED instead of being re-claimed after every lease.
      uow.inTransaction(() -> markRetryOrFailed(entry, e.toString()));
      return;
    }
    // The message is with TI now, so a failure to record that is never counted as a failed send.
    // The entry stays SENDING; after the lease it is sent again with the same eBankEventRef and
    // TheirRef. ASSUMPTION(Q40): TI ignores or refuses the duplicate; the enquiry before re-send
    // (ground rule 7) is task T9.
    uow.inTransaction(() -> markSent(entry, receipt));
  }

  private OutboundMessage assemble(OutboxEntry entry) {
    TradeEvent event = txns.event(entry.eventId()).orElseThrow();
    TradeTxn txn = txns.findById(event.txnId()).orElseThrow();
    var refs = new ChannelRefs(txn.eBankMasterRef(), event.eBankEventRef(), entry.theirRef());
    if (event.type() == EventType.CREATE) {
      return new FinanceSubmission(
          details.create(event.id()).orElseThrow(), txn.customer(), refs, event.createdAt());
    }
    return new RepaySubmission(
        details.repay(event.id()).orElseThrow(),
        txn.customer(),
        txn.tiMasterRef().orElseThrow(),
        refs,
        event.createdAt());
  }

  private Void markSent(OutboxEntry entry, TradeGateway.SendReceipt receipt) {
    Instant now = clock.instant();
    outbox.update(
        new OutboxEntry(entry.id(), entry.eventId(), entry.theirRef(), entry.operation(),
            OutboxStatus.SENT, entry.attempts() + 1, entry.nextAttemptAt(), Optional.of(now),
            Optional.of(receipt.destination()), Optional.empty(), entry.replyStatus(),
            entry.replyDetails(), entry.repliedAt(), entry.createdAt(), entry.version()),
        Optional.of(receipt.body()));
    TradeEvent event = txns.event(entry.eventId()).orElseThrow();
    txns.update(event.moveTo(EventStatus.SENT, Optional.empty(), Optional.empty(), Optional.empty(), now));
    return null;
  }

  private Void markRetryOrFailed(OutboxEntry entry, String error) {
    Instant now = clock.instant();
    int attempts = entry.attempts() + 1;
    boolean giveUp = attempts >= MAX_ATTEMPTS;
    // 30 s, 1 min, 2 min, 4 min between attempts.
    Instant next = now.plus(Duration.ofSeconds(30L << Math.min(attempts - 1, 4)));
    outbox.update(
        new OutboxEntry(entry.id(), entry.eventId(), entry.theirRef(), entry.operation(),
            giveUp ? OutboxStatus.FAILED : OutboxStatus.PENDING, attempts, next, Optional.empty(),
            Optional.empty(), Optional.of(error), entry.replyStatus(), entry.replyDetails(),
            entry.repliedAt(), entry.createdAt(), entry.version()),
        Optional.empty());
    if (giveUp) {
      TradeEvent event = txns.event(entry.eventId()).orElseThrow();
      txns.update(event.moveTo(EventStatus.FAILED, Optional.empty(), Optional.empty(),
          Optional.of("Could not deliver to TI: " + error), now));
      if (event.type() == EventType.CREATE) {
        TradeTxn txn = txns.findById(event.txnId()).orElseThrow();
        txns.update(txn.withStatus(TxnStatus.FAILED, now));
      }
    }
    return null;
  }
}
