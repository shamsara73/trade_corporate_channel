package poc.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Builds the message to send from a template in resources/samples/ with fresh references and dates.
 *
 * <p>The default, TFFINNEW, follows the message the bank sent successfully by hand (2026-10-23):
 * a ServiceRequest envelope with ReplyFormat FULL, so TI answers at once with a ServiceResponse.
 */
public final class SampleMessage {
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter POC_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
  private static final int TENOR_DAYS = 60; // matches FinancePeriod/TenorDays in the TFFINNEW template

  /**
   * References, as the bank defines them: {@code eBankMasterRef} is the channel's transaction number,
   * {@code eBankEventRef} the channel's number for this event (issue, claim, ...), and {@code TheirRef}
   * the message reference, unique for every message delivered (a re-send gets a new one).
   */
  public record Built(
      String messageName, String xml, String theirReference, String messageRef, String eBankMasterRef, String eBankEventRef) {}

  /** Customer data comes from configuration so no real customer is committed with the template. */
  public record Party(String customer, String customerName, String behalfOfBranch) {}

  /** TargetSystem = TI zone id, SourceSystem = this system's id, tiUser = Credentials/Name. */
  public record Header(String targetSystem, String sourceSystem, String tiUser) {}

  private final String messageName;
  private final String template;
  private final Party party;
  private final Header header;
  private final Clock clock;
  private final RandomGenerator random;

  public SampleMessage(String messageName, Party party, Header header, Clock clock, RandomGenerator random) {
    this.messageName = messageName;
    this.party = party;
    this.header = header;
    this.clock = clock;
    this.random = random;
    try (InputStream in = SampleMessage.class.getResourceAsStream("/samples/" + messageName + ".xml")) {
      if (in == null) {
        throw new IllegalArgumentException("no template samples/" + messageName + ".xml");
      }
      this.template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Built build() {
    String suffix = HexFormat.of().withUpperCase().toHexDigits((byte) random.nextInt(256));
    String their = "POC-" + POC_STAMP.format(clock.instant()) + "-" + suffix; // 21 characters; TheirReference allows 34
    // ASSUMPTION(Q34): the shape of the bank's samples ("TF" + 14 characters, and 14 characters). The two
    // are independent numbers (the bank's test used TF2606000000XX02 with event 2606100000XX02).
    String master = "TF" + STAMP.format(clock.instant()) + suffix;
    String event = STAMP.format(clock.instant()) + HexFormat.of().withUpperCase().toHexDigits((byte) random.nextInt(256));
    String messageRef = "MSG-" + POC_STAMP.format(clock.instant()) + "-" + HexFormat.of().withUpperCase().toHexDigits(random.nextInt()); // 27 characters; TheirRef allows 35
    LocalDate start = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    Map<String, String> values = new java.util.HashMap<>();
    values.put("TheirReference", their);
    values.put("eBankMasterRef", master);
    values.put("eBankEventRef", event);
    values.put("TheirRef", messageRef);
    values.put("CorrelationId", messageRef); // the bank's test used the message reference here too
    values.put("Customer", party.customer());
    values.put("CustomerName", party.customerName());
    values.put("BehalfOfBranch", party.behalfOfBranch());
    values.put("TargetSystem", header.targetSystem());
    values.put("SourceSystem", header.sourceSystem());
    values.put("TiUser", header.tiUser());
    values.put("StartDate", start.toString());
    values.put("MaturityDate", start.plusDays(TENOR_DAYS).toString());
    String xml = template;
    for (var e : values.entrySet()) {
      xml = xml.replace("${" + e.getKey() + "}", xmlEscape(e.getValue()));
    }
    return new Built(messageName, xml, their, messageRef, master, event);
  }

  private static String xmlEscape(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
