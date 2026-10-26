package com.bank.tradechannel.domain.txn;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where an event stands. PENDING: waiting in the outbox. SENT: on TI's queue. RECEIVED: TI answered
 * SUCCEEDED and created its event. COMPLETED / REJECTED: the back office decided (TI's ACK or REJ).
 * FAILED: TI refused the message, or it could not be delivered.
 */
public enum EventStatus {
  PENDING,
  SENT,
  RECEIVED,
  COMPLETED,
  REJECTED,
  FAILED;

  /** Still waiting for TI: no other event may start on the transaction (ground rule 7). */
  public boolean inFlight() {
    return this == PENDING || this == SENT || this == RECEIVED;
  }

  /**
   * Allowed moves. TI may send a further ACK or REJ after its first decision, so COMPLETED and
   * REJECTED may replace each other (the last word from TI wins). A late reply never moves an event
   * back.
   */
  public boolean canMoveTo(EventStatus next) {
    return allowedFrom(this).contains(next);
  }

  private static Set<EventStatus> allowedFrom(EventStatus from) {
    switch (from) {
      case PENDING:
        // TI may answer before the dispatcher has recorded the send.
        return EnumSet.of(SENT, RECEIVED, COMPLETED, REJECTED, FAILED);
      case SENT:
        return EnumSet.of(RECEIVED, COMPLETED, REJECTED, FAILED);
      case RECEIVED:
        return EnumSet.of(COMPLETED, REJECTED, FAILED);
      case COMPLETED:
        return EnumSet.of(REJECTED);
      case REJECTED:
        return EnumSet.of(COMPLETED);
      default:
        return EnumSet.noneOf(EventStatus.class);
    }
  }
}
