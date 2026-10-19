package poc.channel;

public enum Origin {
  CHANNEL,
  TI,
  /** An acknowledgement or refusal arrived for a reference this app has never seen. */
  UNKNOWN
}
