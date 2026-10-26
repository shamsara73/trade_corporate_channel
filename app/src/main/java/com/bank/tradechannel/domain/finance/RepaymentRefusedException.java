package com.bank.tradechannel.domain.finance;

import java.util.Map;
import java.util.Optional;

/** A REPAY event cannot start: either not allowed now, or the request breaks a rule. */
public final class RepaymentRefusedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final transient Optional<String> reason;
  private final transient Map<String, String> fieldErrors;

  public RepaymentRefusedException(Optional<String> reason, Map<String, String> fieldErrors) {
    super(reason.orElse("repayment refused: " + fieldErrors));
    this.reason = reason;
    this.fieldErrors = Map.copyOf(fieldErrors);
  }

  /** Why no repayment may start now (message key), if that is the cause. */
  public Optional<String> reason() {
    return reason;
  }

  /** Field name to message key. */
  public Map<String, String> fieldErrors() {
    return fieldErrors;
  }
}
