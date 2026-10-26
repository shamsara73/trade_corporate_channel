package com.bank.tradechannel.persistence;

import static com.bank.tradechannel.persistence.Db.id;
import static com.bank.tradechannel.persistence.Db.utc;

import com.bank.tradechannel.domain.txn.Customer;
import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.StaleUpdateException;
import com.bank.tradechannel.domain.txn.TiDocument;
import com.bank.tradechannel.domain.txn.TradeEvent;
import com.bank.tradechannel.domain.txn.TradeTxn;
import com.bank.tradechannel.domain.txn.TxnStatus;
import com.bank.tradechannel.domain.txn.TxnStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Tables trade_txn, trade_event and ti_document. */
public final class JdbcTxnStore implements TxnStore {
  private static final String TXN_COLUMNS =
      "t.id, t.customer_mnemonic, t.customer_name, t.behalf_of_branch, t.product_code, "
          + "t.e_bank_master_ref, t.their_reference, t.status, t.currency_code, t.finance_amount, "
          + "t.outstanding_amount, t.start_date, t.maturity_date, t.ti_master_ref, t.ti_status, "
          + "t.created_by, t.created_at, t.updated_at, t.row_version";
  private static final String EVENT_COLUMNS =
      "e.id, e.txn_id, e.event_type, e.event_seq, e.e_bank_event_ref, e.status, e.ti_event_ref, "
          + "e.bank_comment, e.error_text, e.created_by, e.created_at, e.updated_at, e.completed_at, "
          + "e.row_version";

  private final JdbcClient jdbc;

  public JdbcTxnStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insert(TradeTxn t) {
    var insert =
        jdbc.sql(
            "INSERT INTO trade_txn (id, customer_mnemonic, customer_name, behalf_of_branch, product_code,"
                + " e_bank_master_ref, their_reference, status, currency_code, finance_amount,"
                + " outstanding_amount, start_date, maturity_date, ti_master_ref, ti_status, created_by,"
                + " created_at, updated_at, row_version)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            id(t.id()), t.customer().mnemonic(), t.customer().name(), t.customer().behalfOfBranch(),
            t.productCode(), t.eBankMasterRef(), t.theirReference(), t.status().name(),
            t.currencyCode(), t.financeAmount(), t.outstanding().orElse(null), t.startDate(),
            t.maturityDate(), Db.orNull(t.tiMasterRef()), Db.orNull(t.tiStatus()), t.createdBy(),
            utc(t.createdAt()), utc(t.updatedAt()), t.version());
    Db.insertUnique(insert, "eBankMasterRef " + t.eBankMasterRef());
  }

  @Override
  public void update(TradeTxn t) {
    int rows =
        jdbc.sql(
                "UPDATE trade_txn SET status = ?, finance_amount = ?, outstanding_amount = ?,"
                    + " maturity_date = ?, ti_master_ref = ?, ti_status = ?, updated_at = ?,"
                    + " row_version = row_version + 1 WHERE id = ? AND row_version = ?")
            .params(
                t.status().name(), t.financeAmount(), t.outstanding().orElse(null), t.maturityDate(),
                Db.orNull(t.tiMasterRef()), Db.orNull(t.tiStatus()), utc(t.updatedAt()), id(t.id()),
                t.version())
            .update();
    if (rows != 1) {
      throw new StaleUpdateException("transaction " + t.eBankMasterRef());
    }
  }

  @Override
  public Optional<TradeTxn> find(String customerMnemonic, UUID txnId) {
    return jdbc.sql("SELECT " + TXN_COLUMNS + " FROM trade_txn t WHERE t.id = ? AND t.customer_mnemonic = ?")
        .params(id(txnId), customerMnemonic)
        .query(JdbcTxnStore::txn)
        .optional();
  }

  @Override
  public Optional<TradeTxn> findById(UUID txnId) {
    return jdbc.sql("SELECT " + TXN_COLUMNS + " FROM trade_txn t WHERE t.id = ?")
        .param(id(txnId))
        .query(JdbcTxnStore::txn)
        .optional();
  }

  @Override
  public List<TradeTxn> list(String customerMnemonic) {
    return jdbc.sql(
            "SELECT " + TXN_COLUMNS
                + " FROM trade_txn t WHERE t.customer_mnemonic = ? ORDER BY t.created_at DESC")
        .param(customerMnemonic)
        .query(JdbcTxnStore::txn)
        .list();
  }

  @Override
  public void insert(TradeEvent e) {
    var insert =
        jdbc.sql(
            "INSERT INTO trade_event (id, txn_id, event_type, event_seq, e_bank_event_ref, status,"
                + " ti_event_ref, bank_comment, error_text, created_by, created_at, updated_at,"
                + " completed_at, row_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            id(e.id()), id(e.txnId()), e.type().name(), e.seq(), e.eBankEventRef(), e.status().name(),
            Db.orNull(e.tiEventRef()), Db.clip(e.bankComment(), 500), Db.clip(e.errorText(), 2000),
            e.createdBy(), utc(e.createdAt()), utc(e.updatedAt()), utc(e.completedAt()), e.version());
    Db.insertUnique(insert, "eBankEventRef " + e.eBankEventRef());
  }

  @Override
  public void update(TradeEvent e) {
    int rows =
        jdbc.sql(
                "UPDATE trade_event SET status = ?, ti_event_ref = ?, bank_comment = ?, error_text = ?,"
                    + " updated_at = ?, completed_at = ?, row_version = row_version + 1"
                    + " WHERE id = ? AND row_version = ?")
            .params(
                e.status().name(), Db.orNull(e.tiEventRef()), Db.clip(e.bankComment(), 500),
                Db.clip(e.errorText(), 2000), utc(e.updatedAt()), utc(e.completedAt()), id(e.id()),
                e.version())
            .update();
    if (rows != 1) {
      throw new StaleUpdateException("event " + e.eBankEventRef());
    }
  }

  @Override
  public List<TradeEvent> events(UUID txnId) {
    return jdbc.sql("SELECT " + EVENT_COLUMNS + " FROM trade_event e WHERE e.txn_id = ? ORDER BY e.event_seq")
        .param(id(txnId))
        .query(JdbcTxnStore::event)
        .list();
  }

  @Override
  public Optional<TradeEvent> event(UUID eventId) {
    return jdbc.sql("SELECT " + EVENT_COLUMNS + " FROM trade_event e WHERE e.id = ?")
        .param(id(eventId))
        .query(JdbcTxnStore::event)
        .optional();
  }

  @Override
  public Optional<TradeEvent> eventByRefs(String eBankMasterRef, String eBankEventRef) {
    return jdbc.sql(
            "SELECT " + EVENT_COLUMNS + " FROM trade_event e JOIN trade_txn t ON t.id = e.txn_id"
                + " WHERE t.e_bank_master_ref = ? AND e.e_bank_event_ref = ?")
        .params(eBankMasterRef, eBankEventRef)
        .query(JdbcTxnStore::event)
        .optional();
  }

  @Override
  public void insert(TiDocument d, byte[] content) {
    jdbc.sql(
            "INSERT INTO ti_document (id, event_id, document_id, description, mime_type, content,"
                + " document_ref, received_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            id(d.id()), id(d.eventId()), Db.orNull(d.documentId()), Db.orNull(d.description()),
            d.mimeType(), content, Db.orNull(d.documentRef()), utc(d.receivedAt()))
        .update();
  }

  @Override
  public List<TiDocument> documents(UUID txnId) {
    return jdbc.sql(
            "SELECT d.id, d.event_id, d.document_id, d.description, d.mime_type, d.document_ref,"
                + " d.received_at FROM ti_document d JOIN trade_event e ON e.id = d.event_id"
                + " WHERE e.txn_id = ? ORDER BY d.received_at")
        .param(id(txnId))
        .query(JdbcTxnStore::document)
        .list();
  }

  @Override
  public Optional<DocumentContent> documentContent(String customerMnemonic, UUID txnId, UUID documentId) {
    return jdbc.sql(
            "SELECT d.id, d.event_id, d.document_id, d.description, d.mime_type, d.document_ref,"
                + " d.received_at, d.content FROM ti_document d"
                + " JOIN trade_event e ON e.id = d.event_id JOIN trade_txn t ON t.id = e.txn_id"
                + " WHERE d.id = ? AND t.id = ? AND t.customer_mnemonic = ?")
        .params(id(documentId), id(txnId), customerMnemonic)
        .query((rs, n) -> new DocumentContent(document(rs, n), rs.getBytes("content")))
        .optional();
  }

  private static TradeTxn txn(ResultSet rs, int row) throws SQLException {
    return new TradeTxn(
        Db.uuid(rs, "id"),
        new Customer(rs.getString("customer_mnemonic"), Db.orEmpty(rs, "customer_name"),
            Db.orEmpty(rs, "behalf_of_branch")),
        rs.getString("product_code"),
        rs.getString("e_bank_master_ref"),
        rs.getString("their_reference"),
        TxnStatus.valueOf(rs.getString("status")),
        rs.getString("currency_code").trim(),
        Db.amount(rs, "finance_amount", rs.getString("currency_code")).orElseThrow(),
        Db.amount(rs, "outstanding_amount", rs.getString("currency_code")),
        rs.getObject("start_date", java.time.LocalDate.class),
        rs.getObject("maturity_date", java.time.LocalDate.class),
        Db.text(rs, "ti_master_ref"),
        Db.text(rs, "ti_status"),
        rs.getString("created_by"),
        Db.instant(rs, "created_at"),
        Db.instant(rs, "updated_at"),
        rs.getInt("row_version"));
  }

  private static TradeEvent event(ResultSet rs, int row) throws SQLException {
    return new TradeEvent(
        Db.uuid(rs, "id"),
        Db.uuid(rs, "txn_id"),
        EventType.valueOf(rs.getString("event_type")),
        rs.getInt("event_seq"),
        rs.getString("e_bank_event_ref"),
        EventStatus.valueOf(rs.getString("status")),
        Db.text(rs, "ti_event_ref"),
        Db.text(rs, "bank_comment"),
        Db.text(rs, "error_text"),
        rs.getString("created_by"),
        Db.instant(rs, "created_at"),
        Db.instant(rs, "updated_at"),
        Db.optionalInstant(rs, "completed_at"),
        rs.getInt("row_version"));
  }

  private static TiDocument document(ResultSet rs, int row) throws SQLException {
    return new TiDocument(
        Db.uuid(rs, "id"),
        Db.uuid(rs, "event_id"),
        Db.orEmpty(rs, "document_id"),
        Db.orEmpty(rs, "description"),
        rs.getString("mime_type"),
        Db.orEmpty(rs, "document_ref"),
        Db.instant(rs, "received_at"));
  }
}
