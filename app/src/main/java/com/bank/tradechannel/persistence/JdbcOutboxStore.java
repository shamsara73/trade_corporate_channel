package com.bank.tradechannel.persistence;

import static com.bank.tradechannel.persistence.Db.id;
import static com.bank.tradechannel.persistence.Db.utc;

import com.bank.tradechannel.domain.txn.OutboxEntry;
import com.bank.tradechannel.domain.txn.OutboxEntry.OutboxStatus;
import com.bank.tradechannel.domain.txn.OutboxStore;
import com.bank.tradechannel.domain.txn.StaleUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Table outbox_msg. Claiming follows ADR 0003: find candidates with a plain SELECT, then claim each
 * with an UPDATE that only succeeds if the row is unchanged, so two dispatchers never send the same
 * message.
 */
public final class JdbcOutboxStore implements OutboxStore {
  private static final String COLUMNS =
      "id, event_id, their_ref, operation, status, attempts, next_attempt_at, sent_at, destination,"
          + " last_error, reply_status, reply_details, replied_at, created_at, row_version";

  private final JdbcClient jdbc;

  public JdbcOutboxStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insert(OutboxEntry e) {
    var insert =
        jdbc.sql(
            "INSERT INTO outbox_msg (id, event_id, their_ref, operation, status, attempts,"
                + " next_attempt_at, created_at, row_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            id(e.id()), id(e.eventId()), e.theirRef(), e.operation(), e.status().name(),
            e.attempts(), utc(e.nextAttemptAt()), utc(e.createdAt()), e.version());
    Db.insertUnique(insert, "TheirRef " + e.theirRef());
  }

  @Override
  public List<OutboxEntry> claimDue(Instant now, Instant staleBefore, int max) {
    List<OutboxEntry> candidates =
        jdbc.sql(
                "SELECT " + COLUMNS + " FROM outbox_msg"
                    + " WHERE (status = 'PENDING' AND next_attempt_at <= ?)"
                    + " OR (status = 'SENDING' AND claimed_at < ?)"
                    + " ORDER BY created_at OFFSET 0 ROWS FETCH NEXT " + max + " ROWS ONLY")
            .params(utc(now), utc(staleBefore))
            .query(JdbcOutboxStore::entry)
            .list();
    List<OutboxEntry> claimed = new ArrayList<>();
    for (OutboxEntry c : candidates) {
      int rows =
          jdbc.sql(
                  "UPDATE outbox_msg SET status = 'SENDING', claimed_at = ?, row_version = row_version + 1"
                      + " WHERE id = ? AND row_version = ?")
              .params(utc(now), id(c.id()), c.version())
              .update();
      if (rows == 1) {
        claimed.add(
            new OutboxEntry(c.id(), c.eventId(), c.theirRef(), c.operation(), OutboxStatus.SENDING,
                c.attempts(), c.nextAttemptAt(), c.sentAt(), c.destination(), c.lastError(),
                c.replyStatus(), c.replyDetails(), c.repliedAt(), c.createdAt(), c.version() + 1));
      }
    }
    return claimed;
  }

  @Override
  public void update(OutboxEntry e, Optional<String> body) {
    int rows =
        jdbc.sql(
                "UPDATE outbox_msg SET status = ?, attempts = ?, next_attempt_at = ?, sent_at = ?,"
                    + " destination = ?, last_error = ?, reply_status = ?, reply_details = ?,"
                    + " replied_at = ?, row_version = row_version + 1 WHERE id = ? AND row_version = ?")
            .params(
                e.status().name(), e.attempts(), utc(e.nextAttemptAt()), utc(e.sentAt()),
                Db.clip(e.destination(), 512), Db.clip(e.lastError(), 2000), Db.orNull(e.replyStatus()),
                Db.clip(e.replyDetails(), 2000), utc(e.repliedAt()), id(e.id()), e.version())
            .update();
    if (rows != 1) {
      throw new StaleUpdateException("outbox message " + e.theirRef());
    }
    body.ifPresent(
        b -> jdbc.sql("UPDATE outbox_msg SET body = ? WHERE id = ?").params(b, id(e.id())).update());
  }

  @Override
  public Optional<OutboxEntry> findByTheirRef(String theirRef) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM outbox_msg WHERE their_ref = ?")
        .param(theirRef)
        .query(JdbcOutboxStore::entry)
        .optional();
  }

  @Override
  public List<OutboxEntry> forEvent(UUID eventId) {
    return jdbc.sql("SELECT " + COLUMNS + " FROM outbox_msg WHERE event_id = ? ORDER BY created_at")
        .param(id(eventId))
        .query(JdbcOutboxStore::entry)
        .list();
  }

  private static OutboxEntry entry(ResultSet rs, int row) throws SQLException {
    return new OutboxEntry(
        Db.uuid(rs, "id"),
        Db.uuid(rs, "event_id"),
        rs.getString("their_ref"),
        rs.getString("operation"),
        OutboxStatus.valueOf(rs.getString("status")),
        rs.getInt("attempts"),
        Db.instant(rs, "next_attempt_at"),
        Db.optionalInstant(rs, "sent_at"),
        Db.text(rs, "destination"),
        Db.text(rs, "last_error"),
        Db.text(rs, "reply_status"),
        Db.text(rs, "reply_details"),
        Db.optionalInstant(rs, "replied_at"),
        Db.instant(rs, "created_at"),
        rs.getInt("row_version"));
  }
}
