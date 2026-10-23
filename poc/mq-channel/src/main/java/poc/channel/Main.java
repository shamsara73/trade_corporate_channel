package poc.channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Clock;
import java.util.Set;

/**
 * Commands:
 *
 * <pre>
 *   send     send one sample message (POC_MESSAGE, default TFFINNEW), record its TheirReference, exit
 *   listen   listen for TFUTIACK / TFUTIREJ / TFUTICRT; type send, list or quit at the prompt
 * </pre>
 */
public final class Main {
  public static void main(String[] args) throws IOException {
    String command = args.length == 0 ? "listen" : args[0];
    Config config = Config.from(System.getenv());
    Clock clock = Clock.systemUTC();
    ReferenceStore store = new ReferenceStore(config.referencesFile(), clock);
    SampleMessage samples = new SampleMessage(config.message(), config.party(), clock, new java.util.Random());

    switch (command) {
      case "send" -> {
        try (MessageBus bus = new MqBus(config)) {
          send(bus, config, store, samples);
        }
      }
      case "listen" -> {
        MessageRouter router = new MessageRouter(store, config.unprocessedLog(), clock, Main::log);
        try (MessageBus bus = new MqBus(config)) {
          // The same queue name twice would make two competing consumers, so listen once.
          for (String queue : Set.of(config.ackRejQueue(), config.crtQueue())) {
            bus.listen(queue, router::handle);
            log("listening on " + queue);
          }
          prompt(bus, config, store, samples);
        }
      }
      default -> {
        System.err.println("usage: send | listen");
        System.exit(2);
      }
    }
  }

  private static void send(MessageBus bus, Config config, ReferenceStore store, SampleMessage samples) {
    SampleMessage.Built message = samples.build();
    bus.send(config.sendQueue(), message.xml());
    // Saved after the send: if the send fails nothing is recorded for a message TI never got.
    store.recordSent(message.messageName(), message.theirReference(), message.messageRef(), message.eBankMasterRef(), message.eBankEventRef());
    log("sent " + message.messageName() + " to " + config.sendQueue() + ", TheirReference " + message.theirReference()
        + ", TheirRef " + message.messageRef() + ", eBankMasterRef " + message.eBankMasterRef() + ", eBankEventRef " + message.eBankEventRef());
  }

  private static void prompt(MessageBus bus, Config config, ReferenceStore store, SampleMessage samples) throws IOException {
    log("commands: send | list | quit");
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    for (String line = in.readLine(); line != null; line = in.readLine()) {
      switch (line.trim()) {
        case "send" -> send(bus, config, store, samples);
        case "list" -> store.all().forEach(r -> System.out.printf("%s  %s  %s  %s%n", r.theirReference(), r.origin(), r.status(), r.updatedAt()));
        case "quit" -> {
          return;
        }
        default -> log("unknown command");
      }
    }
    // Stdin closed (for example run as a service): keep listening until the process is stopped.
    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void log(String text) {
    System.out.println(java.time.LocalTime.now().withNano(0) + " " + text);
  }
}
