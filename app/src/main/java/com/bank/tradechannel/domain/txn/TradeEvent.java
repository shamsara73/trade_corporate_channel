package com.bank.tradechannel.domain.txn;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * One instruction on a transaction.
 *
 * @param seq 1 for the CREATE event, then 2, 3, ... in the order they were started
 * @param eBankEventRef the channel's event number, sent with the event and echoed by TI
 * @param tiEventRef TI's event reference (for example CRE001), known once TI completed the event
 * @param bankComment TI's comment on its decision
 * @param errorText why the event failed, as TI or the transport reported it
 */
public record TradeEvent(
    UUID id,
    UUID txnId,
    EventType type,
    int seq,
    String eBankEventRef,
    EventStatus status,
    Optional<String> tiEventRef,
    Optional<String> bankComment,
    Optional<String> errorText,
    String createdBy,
    Instant createdAt,
    Instant updatedAt,
    Optional<Instant> completedAt,
    int version) {

  /** Moves to {@code next} if the state machine allows it; otherwise returns this event unchanged. */
  public TradeEvent moveTo(
      EventStatus next,
      Optional<String> newTiEventRef,
      Optional<String> comment,
      Optional<String> error,
      Instant at) {
    if (!status.canMoveTo(next)) {
      return this;
    }
    boolean decided = next == EventStatus.COMPLETED || next == EventStatus.REJECTED;
    return new TradeEvent(id, txnId, type, seq, eBankEventRef, next,
        newTiEventRef.isPresent() ? newTiEventRef : tiEventRef,
        comment.isPresent() ? comment : bankComment,
        error.isPresent() ? error : errorText,
        createdBy, createdAt, at, decided ? Optional.of(at) : completedAt, version);
  }
}
