package com.bank.tradechannel.domain.finance;

/** {@code RepaymentAction} of TFFSARPY (codes from TI's XSD). */
public enum RepaymentAction {
  FINAL("F"),
  INTEREST_THEN_PRINCIPAL("I"),
  LAST_PAY_THEN_TRANSFER("T"),
  MINIMUM("M");

  private final String code;

  RepaymentAction(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
