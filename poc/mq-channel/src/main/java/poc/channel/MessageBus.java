package poc.channel;

import java.util.function.Consumer;

/** The two things the app needs from the message broker. */
public interface MessageBus extends AutoCloseable {
  void send(String queue, String text);

  /** Calls {@code handler} for every message on the queue until the bus is closed. */
  void listen(String queue, Consumer<String> handler);

  @Override
  void close();
}
