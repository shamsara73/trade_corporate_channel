package com.bank.tradechannel.domain.txn;

/** Someone else changed the row since it was read; the caller should read again and retry. */
public final class StaleUpdateException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public StaleUpdateException(String what) {
    super(what + " was changed by someone else");
  }
}
