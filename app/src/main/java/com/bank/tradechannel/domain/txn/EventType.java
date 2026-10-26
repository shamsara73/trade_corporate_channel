package com.bank.tradechannel.domain.txn;

/** The events of a Finance Standalone (FSA) transaction and the TI message that carries each. */
public enum EventType {
  /** Request the finance: TFFINNEW. */
  CREATE("TFFINNEW"),
  /** Repay all or part of it: TFFSARPY. */
  REPAY("TFFSARPY");

  private final String operation;

  EventType(String operation) {
    this.operation = operation;
  }

  /** TI's message name, sent as RequestHeader/Operation. */
  public String operation() {
    return operation;
  }
}
