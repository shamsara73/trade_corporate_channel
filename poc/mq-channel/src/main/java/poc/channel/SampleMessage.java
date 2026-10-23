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
 * <p>The default, TFFINNEW, is the bank's working sample: a bare message element with no
 * ServiceRequest envelope. ASSUMPTION(POC-5): the send queue takes that bare form for every
 * message, so the TFIGTAPP template has the same shape.
 */
public final class SampleMessage {
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyMMddHHmmss").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter POC_STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
  private static final int TENOR_DAYS = 60; // matches FinancePeriod/TenorDays in the TFFINNEW template

  public record Built(String messageName, String xml, String theirReference, String eBankMasterRef, String eBankEventRef) {}

  /** Customer data comes from configuration so no real customer is committed with the template. */
  public record Party(String customer, String customerName, String behalfOfBranch) {}

  private final String messageName;
  private final String template;
  private final Party party;
  private final Clock clock;
  private final RandomGenerator random;

  public SampleMessage(String messageName, Party party, Clock clock, RandomGenerator random) {
    this.messageName = messageName;
    this.party = party;
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
    // Same shape as the working sample (event 14 characters, master = "TF" + event); both allow at least 20.
    String event = STAMP.format(clock.instant()) + suffix;
    String master = "TF" + event;
    LocalDate start = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    Map<String, String> values =
        Map.of(
            "TheirReference", their,
            "eBankMasterRef", master,
            "eBankEventRef", event,
            "Customer", party.customer(),
            "CustomerName", party.customerName(),
            "BehalfOfBranch", party.behalfOfBranch(),
            "StartDate", start.toString(),
            "MaturityDate", start.plusDays(TENOR_DAYS).toString());
    String xml = template;
    for (var e : values.entrySet()) {
      xml = xml.replace("${" + e.getKey() + "}", xmlEscape(e.getValue()));
    }
    return new Built(messageName, xml, their, master, event);
  }

  private static String xmlEscape(String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
