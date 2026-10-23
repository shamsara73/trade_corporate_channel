package poc.channel;

import java.nio.file.Path;
import java.util.Map;

/**
 * Settings from environment variables, falling back to the defaults of the IBM MQ developer
 * container (queue manager QM1, channel DEV.APP.SVRCONN, queues DEV.QUEUE.1 to 3).
 */
public record Config(
    String host,
    int port,
    String channel,
    String queueManager,
    String user,
    String password,
    String sendQueue,
    String ackRejQueue,
    String crtQueue,
    boolean rfh2,
    String message,
    SampleMessage.Party party,
    Path referencesFile,
    Path unprocessedLog) {

  public static Config from(Map<String, String> env) {
    return new Config(
        env.getOrDefault("MQ_HOST", "localhost"),
        Integer.parseInt(env.getOrDefault("MQ_PORT", "1414")),
        env.getOrDefault("MQ_CHANNEL", "DEV.APP.SVRCONN"),
        env.getOrDefault("MQ_QMGR", "QM1"),
        env.getOrDefault("MQ_USER", "app"),
        env.getOrDefault("MQ_PASSWORD", ""),
        env.getOrDefault("MQ_QUEUE_SEND", "DEV.QUEUE.1"),
        env.getOrDefault("MQ_QUEUE_ACKREJ", "DEV.QUEUE.2"),
        env.getOrDefault("MQ_QUEUE_CRT", "DEV.QUEUE.3"),
        Boolean.parseBoolean(env.getOrDefault("MQ_RFH2", "false")),
        env.getOrDefault("POC_MESSAGE", "TFFINNEW"),
        // Invented defaults: set the real test customer through the environment, never in the template.
        new SampleMessage.Party(
            env.getOrDefault("POC_CUSTOMER", "0009990001"),
            env.getOrDefault("POC_CUSTOMER_NAME", "POC TEST CUSTOMER"),
            env.getOrDefault("POC_BEHALF_OF_BRANCH", "001")),
        Path.of(env.getOrDefault("REFERENCES_FILE", "references.json")),
        Path.of(env.getOrDefault("UNPROCESSED_FILE", "unprocessed.log")));
  }
}
