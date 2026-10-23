package poc.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The "database": a JSON array in one file, rewritten on every change. The send and listener threads
 * share it, so every method is synchronized. Fine for a POC; not a design for production.
 */
public final class ReferenceStore {
  private final Path file;
  private final Clock clock;
  private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  private final Map<String, Reference> byTheirReference = new LinkedHashMap<>();

  public ReferenceStore(Path file, Clock clock) {
    this.file = file;
    this.clock = clock;
    load();
  }

  public synchronized void recordSent(
      String messageName, String theirReference, String messageRef, String eBankMasterRef, String eBankEventRef) {
    put(new Reference(theirReference, Origin.CHANNEL, Status.SENT, eBankMasterRef, eBankEventRef, messageRef, "", "", messageName, now()));
  }

  /** TI created the transaction (TFUTICRT). Keeps an existing entry untouched and says so. */
  public synchronized boolean recordFromTi(OutwardMessage m) {
    if (find(m).isPresent()) {
      return false;
    }
    put(new Reference(keyOf(m), Origin.TI, Status.TI_CREATED, m.eBankMasterRef(), m.eBankEventRef(), "", m.masterRef(), m.eventRef(), m.name(), now()));
    return true;
  }

  /** Marks a reference PROCESSED or REJECTED; an unknown reference is recorded with origin UNKNOWN. */
  public synchronized Reference mark(OutwardMessage m, Status status) {
    Optional<Reference> existing = find(m);
    Reference updated =
        existing
            .map(r -> new Reference(r.theirReference(), r.origin(), status, pick(r.eBankMasterRef(), m.eBankMasterRef()), pick(r.eBankEventRef(), m.eBankEventRef()), r.messageRef(), pick(r.masterRef(), m.masterRef()), pick(r.eventRef(), m.eventRef()), m.name(), now()))
            .orElseGet(() -> new Reference(keyOf(m), Origin.UNKNOWN, status, m.eBankMasterRef(), m.eBankEventRef(), "", m.masterRef(), m.eventRef(), m.name(), now()));
    put(updated);
    return updated;
  }

  /**
   * The channel's transaction and event numbers identify the event exactly, so they are tried first;
   * then the customer reference; then the transaction number alone (an event TI started on our master).
   */
  /**
   * Applies TI's immediate reply, found by the CorrelationId we sent (our TheirRef). A reply that
   * arrives after the outward ACK/REJ does not overwrite the later status.
   */
  public synchronized Optional<Reference> markReply(String correlationId, Status status) {
    Optional<Reference> found =
        byTheirReference.values().stream().filter(r -> !correlationId.isEmpty() && r.messageRef().equals(correlationId)).findFirst();
    found
        .filter(r -> r.status() == Status.SENT)
        .ifPresent(
            r ->
                put(new Reference(r.theirReference(), r.origin(), status, r.eBankMasterRef(), r.eBankEventRef(), r.messageRef(), r.masterRef(), r.eventRef(), "ServiceResponse", now())));
    return found.map(r -> byTheirReference.get(r.theirReference()));
  }

  public synchronized Optional<Reference> find(OutwardMessage m) {
    if (!m.eBankMasterRef().isEmpty() && !m.eBankEventRef().isEmpty()) {
      Optional<Reference> exact =
          byTheirReference.values().stream()
              .filter(r -> r.eBankMasterRef().equals(m.eBankMasterRef()) && r.eBankEventRef().equals(m.eBankEventRef()))
              .findFirst();
      if (exact.isPresent()) {
        return exact;
      }
    }
    if (!m.theirReference().isEmpty() && byTheirReference.containsKey(m.theirReference())) {
      return Optional.of(byTheirReference.get(m.theirReference()));
    }
    if (m.eBankMasterRef().isEmpty()) {
      return Optional.empty();
    }
    return byTheirReference.values().stream().filter(r -> r.eBankMasterRef().equals(m.eBankMasterRef())).findFirst();
  }

  public synchronized List<Reference> all() {
    return List.copyOf(byTheirReference.values());
  }

  private static String keyOf(OutwardMessage m) {
    return m.theirReference().isEmpty() ? m.eBankMasterRef() : m.theirReference();
  }

  private static String pick(String current, String incoming) {
    return incoming.isEmpty() ? current : incoming;
  }

  private String now() {
    return clock.instant().toString();
  }

  private void put(Reference r) {
    byTheirReference.put(r.theirReference(), r);
    save();
  }

  private void load() {
    if (!Files.exists(file)) {
      return;
    }
    try {
      for (Reference r : json.readValue(file.toFile(), Reference[].class)) {
        byTheirReference.put(r.theirReference(), r);
      }
    } catch (IOException e) {
      throw new IllegalStateException("cannot read " + file, e);
    }
  }

  /** Write to a temp file and move it over, so a crash never leaves half a file. */
  private void save() {
    try {
      Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
      json.writeValue(tmp.toFile(), new ArrayList<>(byTheirReference.values()));
      Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("cannot write " + file, e);
    }
  }
}
