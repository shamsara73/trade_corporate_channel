package com.bank.tradechannel.domain.finance;

import java.time.LocalDate;
import java.util.Objects;

/**
 * What the customer asks for in a Finance Standalone request (TFFINNEW), in the shape of the
 * message the bank's TI accepted (docs/reference/working-samples.md, section 2).
 *
 * @param theirReference the customer's own reference, max 34
 * @param financeAmount the amount to finance; TI books this one
 * @param transactionAmount the underlying amount ({@code Amount} in the message); ASSUMPTION(Q37):
 *     equals the finance amount unless the customer gives one
 * @param startDate also sent as the effective date
 * @param tenorDays the finance period in days
 * @param productType TI product type, for example PSF
 */
public record FinanceRequest(
    String theirReference,
    Money financeAmount,
    Money transactionAmount,
    LocalDate startDate,
    int tenorDays,
    boolean trustReceiptLoan,
    InterestFrom interestFrom,
    String productType,
    String goodsDescription,
    String messageToBank,
    String additionalInformation) {

  public FinanceRequest {
    Objects.requireNonNull(theirReference, "theirReference");
    Objects.requireNonNull(financeAmount, "financeAmount");
    Objects.requireNonNull(transactionAmount, "transactionAmount");
    Objects.requireNonNull(startDate, "startDate");
    Objects.requireNonNull(interestFrom, "interestFrom");
    Objects.requireNonNull(productType, "productType");
    goodsDescription = goodsDescription == null ? "" : goodsDescription;
    messageToBank = messageToBank == null ? "" : messageToBank;
    additionalInformation = additionalInformation == null ? "" : additionalInformation;
  }

  /**
   * TI recalculates the maturity as start + tenor and ignores a conflicting date (bank test
   * 2026-10-23), so the channel sends exactly that. ASSUMPTION(Q36)
   */
  public LocalDate maturityDate() {
    return startDate.plusDays(tenorDays);
  }
}
