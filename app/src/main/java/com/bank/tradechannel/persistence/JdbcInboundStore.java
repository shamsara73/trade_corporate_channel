package com.bank.tradechannel.persistence;

import static com.bank.tradechannel.persistence.Db.id;
import static com.bank.tradechannel.persistence.Db.utc;

import com.bank.tradechannel.domain.txn.InboundStore;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Table inbound_msg. */
public final class JdbcInboundStore implements InboundStore {
  private final JdbcClient jdbc;

  public JdbcInboundStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void record(InboundRecord r) {
    jdbc.sql(
            "INSERT INTO inbound_msg (id, received_at, source, kind, operation, message_name,"
                + " correlation_id, e_bank_master_ref, e_bank_event_ref, event_id, outcome, body)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            id(r.id()), utc(r.receivedAt()), r.source(), r.kind(), Db.orNull(r.operation()),
            Db.orNull(r.messageName()), Db.orNull(r.correlationId()), Db.orNull(r.eBankMasterRef()),
            Db.orNull(r.eBankEventRef()), r.eventId().map(Db::id).orElse(null), r.outcome().name(),
            r.body())
        .update();
  }
}
