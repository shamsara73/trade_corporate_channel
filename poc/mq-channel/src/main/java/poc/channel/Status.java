package poc.channel;

/** Where a reference stands. SENT and TI_CREATED are the starting states; the others come from TI. */
public enum Status {
  SENT,
  TI_CREATED,
  PROCESSED,
  REJECTED
}
