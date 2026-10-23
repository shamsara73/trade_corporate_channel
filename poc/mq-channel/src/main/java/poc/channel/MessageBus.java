package poc.channel;

import java.util.function.Consumer;

/** The two things the app needs from the message broker. */
public interface MessageBus extends AutoCloseable {
  /** Puts {@code text} on {@code queue}; a non-empty {@code replyTo} becomes the JMS reply-to. */
  void send(String queue, String text, String replyTo);

  /** Calls {@code handler} for every message on the queue until the bus is closed. */
  void listen(String queue, Consumer<String> handler);

  @Override
  void close();
}
