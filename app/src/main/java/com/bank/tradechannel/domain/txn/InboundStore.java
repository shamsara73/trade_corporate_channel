package com.bank.tradechannel.domain.txn;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Keeps every message read from TI, matched or not. */
public interface InboundStore {
  void record(InboundRecord record);

  /** What happened to a message from TI. */
  enum Outcome {
    MATCHED,
    DUPLICATE,
    UNMATCHED,
    UNREADABLE
  }

  record InboundRecord(
      UUID id,
      Instant receivedAt,
      String source,
      String kind,
      Optional<String> operation,
      Optional<String> messageName,
      Optional<String> correlationId,
      Optional<String> eBankMasterRef,
      Optional<String> eBankEventRef,
      Optional<UUID> eventId,
      Outcome outcome,
      String body) {}
}
