package com.bank.tradechannel.domain.txn;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The outbox of messages to TI (ADR 0003). */
public interface OutboxStore {
  void insert(OutboxEntry entry);

  /**
   * Claims up to {@code max} entries that are due ({@code PENDING} with {@code nextAttemptAt <= now}),
   * or whose claim is older than {@code staleBefore} (a dispatcher that died). A conditional update
   * makes sure each entry goes to one dispatcher only. Returns the claimed entries, now SENDING.
   */
  List<OutboxEntry> claimDue(Instant now, Instant staleBefore, int max);

  /**
   * Saves the entry if its version is unchanged (else {@link StaleUpdateException}); {@code body} is
   * the message text as sent, stored once on delivery.
   */
  void update(OutboxEntry entry, Optional<String> body);

  Optional<OutboxEntry> findByTheirRef(String theirRef);

  List<OutboxEntry> forEvent(UUID eventId);
}
