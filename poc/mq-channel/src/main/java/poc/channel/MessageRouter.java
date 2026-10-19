package poc.channel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.function.Consumer;

/**
 * Decides what each inbound message does to the JSON file. It looks at the message name, not at the
 * queue, so a message on the "wrong" queue is still handled.
 *
 * <p>TI may send further TFUTIACK / TFUTIREJ after the first TFUTICRT, so a later message simply
 * overwrites the status: the last word from TI wins.
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
    OutwardMessage m;
    try {
      m = OutwardMessage.parse(xml);
    } catch (IllegalArgumentException e) {
      park(e.getMessage(), xml);
      return;
    }
    switch (m.name()) {
      case "TFUTIACK" -> report(m, store.mark(m, Status.PROCESSED));
      case "TFUTIREJ" -> report(m, store.mark(m, Status.REJECTED));
      case "TFUTICRT" -> {
        boolean added = store.recordFromTi(m);
        out.accept(
            added
                ? "TFUTICRT: saved " + m.theirReference() + " (TI-created)"
                : "TFUTICRT: " + m.theirReference() + " already known, left as is");
      }
      default -> park("unexpected message name " + m.name(), xml);
    }
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
