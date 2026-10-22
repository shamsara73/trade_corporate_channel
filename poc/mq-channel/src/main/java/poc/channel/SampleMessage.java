package poc.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Builds a TFIGTAPP from the template in resources/tfigtapp-sample.xml with fresh references. */
public final class SampleMessage {
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

  public record Built(String xml, String theirReference, String eBankMasterRef, String eBankEventRef) {}

  private final String template;
  private final Clock clock;

  public SampleMessage(Clock clock) {
    this.clock = clock;
    try (InputStream in = SampleMessage.class.getResourceAsStream("/tfigtapp-sample.xml")) {
      this.template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Built build() {
    String stamp = STAMP.format(clock.instant());
    String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    String their = "POC-" + stamp + "-" + suffix; // 23 characters; TheirReference allows 34
    String master = "CH-POC-" + stamp + "-" + suffix;
    String event = master + "-01";
    String xml =
        template
            .replace("${TheirReference}", their)
            .replace("${eBankMasterRef}", master)
            .replace("${eBankEvent}", event)
            .replace("${CorrelationId}", UUID.randomUUID().toString())
            .replace("${CreationDate}", java.time.LocalDate.ofInstant(clock.instant(), java.time.ZoneOffset.UTC).toString()); // xsd:date, the business date
    return new Built(xml, their, master, event);
  }
}
