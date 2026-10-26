package com.bank.tradechannel.domain.txn;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transactions, their events and TI's documents. Every read a user can trigger is scoped to the
 * customer (ground rule 8). Updates check the version they read and throw {@link
 * StaleUpdateException} if it changed.
 */
public interface TxnStore {
  void insert(TradeTxn txn);

  void update(TradeTxn txn);

  Optional<TradeTxn> find(String customerMnemonic, UUID txnId);

  /** For background work only; never call it with an id from a request. */
  Optional<TradeTxn> findById(UUID txnId);

  List<TradeTxn> list(String customerMnemonic);

  void insert(TradeEvent event);

  void update(TradeEvent event);

  List<TradeEvent> events(UUID txnId);

  Optional<TradeEvent> event(UUID eventId);

  Optional<TradeEvent> eventByRefs(String eBankMasterRef, String eBankEventRef);

  void insert(TiDocument document, byte[] content);

  List<TiDocument> documents(UUID txnId);

  /** The document's content, if it belongs to a transaction of this customer. */
  Optional<DocumentContent> documentContent(String customerMnemonic, UUID txnId, UUID documentId);

  /** A document ready to download. */
  record DocumentContent(TiDocument document, byte[] content) {}
}
