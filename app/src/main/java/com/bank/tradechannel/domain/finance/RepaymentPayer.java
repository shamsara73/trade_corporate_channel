package com.bank.tradechannel.domain.finance;

/** {@code RepaymentPayer} of TFFSARPY: who pays the repayment. */
public enum RepaymentPayer {
  DEBIT_PARTY("D"),
  FINANCE_PARTY("F");

  private final String code;

  RepaymentPayer(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
