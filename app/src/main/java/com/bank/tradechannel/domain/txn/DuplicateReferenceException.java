package com.bank.tradechannel.domain.txn;

/**
 * A generated channel reference (eBankMasterRef, eBankEventRef or TheirRef) is already taken. The
 * references are short (ASSUMPTION(Q34)), so two requests in the same second can draw the same one;
 * the caller retries with new references.
 */
public final class DuplicateReferenceException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DuplicateReferenceException(String what, Throwable cause) {
    super(what + " is already taken", cause);
  }
}
