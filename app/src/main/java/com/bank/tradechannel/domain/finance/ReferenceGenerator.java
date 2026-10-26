package com.bank.tradechannel.domain.finance;

import com.bank.tradechannel.domain.txn.ChannelRefs;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.random.RandomGenerator;

/**
 * Generates the channel's references. ASSUMPTION(Q34): the shape of the bank's samples, "TF" + 14
 * characters for the transaction and an independent 14 characters for each event; the message
 * reference is "MSG-" + timestamp + random (27 of 34 characters). Uniqueness across restarts comes
 * from the database once it exists (task T5).
 */
public final class ReferenceGenerator {
  private static final DateTimeFormatter SHORT =
      DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter LONG =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
  private static final HexFormat HEX = HexFormat.of().withUpperCase();

  private final Clock clock;
  private final RandomGenerator random;

  public ReferenceGenerator(Clock clock, RandomGenerator random) {
    this.clock = clock;
    this.random = random;
  }

  /** References for the first event of a new transaction. */
  public ChannelRefs newTransaction() {
    var now = clock.instant();
    String master = "TF" + SHORT.format(now) + HEX.toHexDigits((byte) random.nextInt(256));
    return newEvent(master);
  }

  /** References for a further event on an existing transaction: a new event and message number. */
  public ChannelRefs newEvent(String eBankMasterRef) {
    String event = SHORT.format(clock.instant()) + HEX.toHexDigits((byte) random.nextInt(256));
    return new ChannelRefs(eBankMasterRef, event, newMessageRef());
  }

  /** A new message reference, for example for a re-send of the same event. */
  public String newMessageRef() {
    return "MSG-" + LONG.format(clock.instant()) + "-" + HEX.toHexDigits(random.nextInt());
  }
}
