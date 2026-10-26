package com.bank.tradechannel.domain.txn;

/**
 * Where a transaction stands, derived from its events. PENDING: the CREATE event is with TI. LIVE:
 * TI booked the finance (CREATE completed). REPAID: nothing outstanding. REJECTED / FAILED: the CREATE
 * event was refused, so the transaction never existed in TI.
 */
public enum TxnStatus {
  PENDING,
  LIVE,
  REPAID,
  REJECTED,
  FAILED;

  public boolean closed() {
    return this == REPAID || this == REJECTED || this == FAILED;
  }
}
