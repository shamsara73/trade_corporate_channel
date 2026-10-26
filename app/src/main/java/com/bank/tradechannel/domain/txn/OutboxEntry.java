package com.bank.tradechannel.domain.txn;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A message waiting for, or done with, delivery to TI.
 *
 * @param theirRef the message reference: unique per delivery, sent as TheirRef and CorrelationId
 * @param attempts deliveries tried so far
 */
public record OutboxEntry(
    UUID id,
    UUID eventId,
    String theirRef,
    String operation,
    OutboxStatus status,
    int attempts,
    Instant nextAttemptAt,
    Optional<Instant> sentAt,
    Optional<String> destination,
    Optional<String> lastError,
    Optional<String> replyStatus,
    Optional<String> replyDetails,
    Optional<Instant> repliedAt,
    Instant createdAt,
    int version) {

  /** PENDING: due for delivery. SENDING: claimed by a dispatcher. SENT: on TI's queue. FAILED: gave up. */
  public enum OutboxStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED
  }
}
