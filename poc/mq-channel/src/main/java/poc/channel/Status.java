package poc.channel;

/**
 * Where a reference stands. SENT and TI_CREATED are the starting states. RECEIVED and FAILED come
 * from TI's immediate ServiceResponse; PROCESSED and REJECTED from its outward message after the
 * back-office decision.
 */
public enum Status {
  SENT,
  RECEIVED,
  FAILED,
  TI_CREATED,
  PROCESSED,
  REJECTED
}
