package com.bank.tradechannel.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * What the customer asks for in a REPAY event (TFFSARPY).
 *
 * @param repaymentReference the customer's reference for the repayment, max 34
 * @param principal the principal repaid; must not exceed what is outstanding
 * @param interest interest paid with it, zero or more, in the same currency
 */
public record RepaymentRequest(
    String repaymentReference,
    RepaymentAction action,
    LocalDate valueDate,
    RepaymentPayer payer,
    Money principal,
    BigDecimal interest) {

  public RepaymentRequest {
    Objects.requireNonNull(repaymentReference, "repaymentReference");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(valueDate, "valueDate");
    Objects.requireNonNull(payer, "payer");
    Objects.requireNonNull(principal, "principal");
    Objects.requireNonNull(interest, "interest");
    if (interest.signum() < 0) {
      throw new IllegalArgumentException("interest must not be negative");
    }
    interest = interest.setScale(principal.amount().scale());
  }

  public BigDecimal total() {
    return principal.amount().add(interest);
  }
}
