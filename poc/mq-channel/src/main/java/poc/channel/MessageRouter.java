package poc.channel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Decides what each inbound message does to the JSON file. It looks at the message name, not at the
 * queue, so a message on the "wrong" queue is still handled.
 *
 * <p>TI may send further ACK / REJ messages after the first CRT, so a later message simply
 * overwrites the status: the last word from TI wins.
 *
 * <p>ASSUMPTION(POC-7): the kind of an outward message is its name's last three letters, ACK, REJ or
 * CRT, read from MessageName or the header Operation. Seen so far: TFUTIACK, TFUTIREJ, TFUTICRT
 * (mappings) and, for a finance, Operation TFFINACK carrying MessageName TFFSAACK (bank test).
 */
public final class MessageRouter {
  private final ReferenceStore store;
  private final Path unprocessedLog;
  private final Clock clock;
  private final Consumer<String> out;

  public MessageRouter(ReferenceStore store, Path unprocessedLog, Clock clock, Consumer<String> out) {
    this.store = store;
    this.unprocessedLog = unprocessedLog;
    this.clock = clock;
    this.out = out;
  }

  /** Never throws for a bad message: it is written to the unprocessed log so the queue does not loop. */
  public void handle(String xml) {
    if (xml.contains("ServiceResponse")) {
      Optional<ServiceResponseSummary> reply = ServiceResponseSummary.of(xml);
      if (reply.isPresent()) {
        handleReply(reply.get());
        return;
      }
    }
    OutwardMessage m;
    try {
      m = OutwardMessage.parse(xml);
    } catch (IllegalArgumentException e) {
      park(e.getMessage(), xml);
      return;
    }
    switch (kind(m)) {
      case "ACK" -> report(m, store.mark(m, Status.PROCESSED));
      case "REJ" -> report(m, store.mark(m, Status.REJECTED));
      case "CRT" -> {
        boolean added = store.recordFromTi(m);
        out.accept(
            added
                ? m.name() + ": saved " + m.theirReference() + " (TI-created)"
                : m.name() + ": " + m.theirReference() + " already known, left as is");
      }
      default -> park("unexpected message name " + m.name() + (m.operation().isEmpty() ? "" : " / " + m.operation()), xml);
    }
  }

  private static String kind(OutwardMessage m) {
    for (String name : new String[] {m.name(), m.operation()}) {
      if (name.length() > 3) {
        String suffix = name.substring(name.length() - 3);
        if (suffix.equals("ACK") || suffix.equals("REJ") || suffix.equals("CRT")) {
          return suffix;
        }
      }
    }
    return "";
  }

  private void handleReply(ServiceResponseSummary reply) {
    Status status = "SUCCEEDED".equals(reply.status()) ? Status.RECEIVED : Status.FAILED;
    Optional<Reference> updated = store.markReply(reply.correlationId(), status);
    out.accept(
        "ServiceResponse: "
            + reply
            + updated.map(r -> " -> " + r.theirReference() + " " + r.status()).orElse(" (no message sent with this CorrelationId)"));
  }

  private void report(OutwardMessage m, Reference r) {
    String note = r.origin() == Origin.UNKNOWN ? " (reference not sent by this app)" : "";
    out.accept(m.name() + ": " + r.theirReference() + " -> " + r.status() + note);
  }

  private void park(String reason, String xml) {
    out.accept("UNPROCESSED: " + reason + " (raw message in " + unprocessedLog + ")");
    try {
      Files.writeString(
          unprocessedLog,
          clock.instant() + " " + reason + System.lineSeparator() + xml + System.lineSeparator() + "----" + System.lineSeparator(),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
