package com.bank.tradechannel.domain.finance;

/** {@code InterestFromAfter} in TFFINNEW: whether interest runs from or after the start date. */
public enum InterestFrom {
  AFTER("A"),
  FROM("F"),
  FROM_EXCLUSIVE("E");

  private final String code;

  InterestFrom(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
