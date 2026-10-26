package com.bank.tradechannel.domain.txn;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * A transaction: one TI master, for now a Finance Standalone (product FSA).
 *
 * @param customer the corporate, as sent in the messages (number, name, branch)
 * @param eBankMasterRef the channel's transaction number, sent with every event
 * @param financeAmount what the customer asked for; replaced by TI's booked amount on completion
 * @param outstanding what is still to repay, known once TI booked the finance
 * @param tiMasterRef TI's master reference, known once TI completed the CREATE event
 * @param tiStatus TI's master status from its last message (for example LIV)
 * @param version optimistic lock: every update must name the version it read
 */
public record TradeTxn(
    UUID id,
    Customer customer,
    String productCode,
    String eBankMasterRef,
    String theirReference,
    TxnStatus status,
    String currencyCode,
    BigDecimal financeAmount,
    Optional<BigDecimal> outstanding,
    LocalDate startDate,
    LocalDate maturityDate,
    Optional<String> tiMasterRef,
    Optional<String> tiStatus,
    String createdBy,
    Instant createdAt,
    Instant updatedAt,
    int version) {

  public TradeTxn withStatus(TxnStatus newStatus, Instant at) {
    return new TradeTxn(id, customer, productCode, eBankMasterRef, theirReference, newStatus,
        currencyCode, financeAmount, outstanding, startDate, maturityDate, tiMasterRef, tiStatus,
        createdBy, createdAt, at, version);
  }

  /** TI's view after it completed an event. */
  public TradeTxn bookedByTi(
      TxnStatus newStatus,
      BigDecimal booked,
      Optional<BigDecimal> newOutstanding,
      LocalDate newMaturity,
      Optional<String> masterRef,
      Optional<String> masterStatus,
      Instant at) {
    return new TradeTxn(id, customer, productCode, eBankMasterRef, theirReference, newStatus,
        currencyCode, booked, newOutstanding, startDate, newMaturity,
        masterRef.isPresent() ? masterRef : tiMasterRef,
        masterStatus.isPresent() ? masterStatus : tiStatus, createdBy, createdAt, at, version);
  }
}
