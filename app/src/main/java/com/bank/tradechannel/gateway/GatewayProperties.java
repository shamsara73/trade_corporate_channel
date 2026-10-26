package com.bank.tradechannel.gateway;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code channel.gateway}. Header values follow the bank's successful test
 * (docs/reference/working-samples.md, section 2).
 *
 * @param mode {@code mq}: send to TI's incoming queue and listen to its outgoing queue(s);
 *     {@code file}: write each message to {@code fileDirectory} and read TI's messages from {@code
 *     inboxDirectory} (development without MQ)
 * @param targetSystem TI zone id ({@code ZONE1} in the bank's test)
 * @param sourceSystem this channel's system id
 * @param tiUser the TI user name sent in Credentials/Name
 * @param simulator in file mode, answers like TI (for demos; never in production)
 */
@ConfigurationProperties("channel.gateway")
public record GatewayProperties(
    Mode mode,
    String targetSystem,
    String sourceSystem,
    String tiUser,
    Path fileDirectory,
    Path inboxDirectory,
    Mq mq,
    Simulator simulator) {

  public GatewayProperties {
    mode = mode == null ? Mode.FILE : mode;
    fileDirectory = fileDirectory == null ? Path.of("sent-messages") : fileDirectory;
    inboxDirectory = inboxDirectory == null ? Path.of("ti-inbox") : inboxDirectory;
    simulator = simulator == null ? new Simulator(false, Duration.ofSeconds(10)) : simulator;
  }

  public enum Mode {
    FILE,
    MQ
  }

  /**
   * IBM MQ client connection. The password should come from the environment, never from a file in
   * the repository.
   *
   * @param replyQueue where TI puts its ServiceResponse (sent as the JMS reply-to)
   * @param outwardQueue where TI puts its outward messages (TFFINACK ...); empty if the same as
   *     {@code replyQueue}
   */
  public record Mq(
      String host,
      int port,
      String channel,
      String queueManager,
      String user,
      String password,
      String sendQueue,
      String replyQueue,
      String outwardQueue) {

    /** Every queue the channel listens to, each once. */
    public Set<String> listenQueues() {
      Set<String> queues = new LinkedHashSet<>();
      for (String q : List.of(nonNull(replyQueue), nonNull(outwardQueue))) {
        if (!q.isBlank()) {
          queues.add(q.trim());
        }
      }
      return queues;
    }

    private static String nonNull(String s) {
      return s == null ? "" : s;
    }
  }

  /**
   * @param enabled answer every message like TI: a ServiceResponse at once, an ACK after {@code
   *     completeAfter}
   */
  public record Simulator(boolean enabled, Duration completeAfter) {
    public Simulator {
      completeAfter = completeAfter == null ? Duration.ofSeconds(10) : completeAfter;
    }
  }
}
