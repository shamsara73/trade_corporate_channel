package com.bank.tradechannel.web.txn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** What the repayment page posts (TFFSARPY); the currency is the finance's, not chosen here. */
public class RepayForm {
  static final String AMOUNT = "\\d{1,15}(\\.\\d{1,3})?";

  @NotBlank
  @Size(max = 34)
  @Pattern(regexp = "[\\p{Print}&&[^<>]]*", message = "{finance.error.characters}")
  private String repaymentReference;

  @NotBlank
  @Pattern(regexp = "FINAL|INTEREST_THEN_PRINCIPAL|LAST_PAY_THEN_TRANSFER|MINIMUM")
  private String action;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate valueDate;

  @NotBlank
  @Pattern(regexp = "DEBIT_PARTY|FINANCE_PARTY")
  private String payer;

  @NotBlank
  @Pattern(regexp = AMOUNT, message = "{finance.error.amount}")
  private String principal;

  @Pattern(regexp = "|" + AMOUNT, message = "{finance.error.amount}")
  private String interest = "";

  public String getRepaymentReference() {
    return repaymentReference;
  }

  public void setRepaymentReference(String repaymentReference) {
    this.repaymentReference = repaymentReference;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public void setValueDate(LocalDate valueDate) {
    this.valueDate = valueDate;
  }

  public String getPayer() {
    return payer;
  }

  public void setPayer(String payer) {
    this.payer = payer;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }

  public String getInterest() {
    return interest;
  }

  public void setInterest(String interest) {
    this.interest = interest;
  }
}
