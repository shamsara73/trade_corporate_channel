package com.bank.tradechannel.gateway;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code channel.gateway}. Header values follow the bank's successful test
 * (docs/reference/working-samples.md, section 2).
 *
 * @param mode {@code file} writes each message to {@code fileDirectory} (development, no MQ);
 *     {@code mq} puts it on TI's incoming queue
 * @param targetSystem TI zone id ({@code ZONE1} in the bank's test)
 * @param sourceSystem this channel's system id
 * @param tiUser the TI user name sent in Credentials/Name
 */
@ConfigurationProperties("channel.gateway")
public record GatewayProperties(
    Mode mode, String targetSystem, String sourceSystem, String tiUser, Path fileDirectory, Mq mq) {

  public enum Mode {
    FILE,
    MQ
  }

  /**
   * IBM MQ client connection. The password comes from the environment, never from a file in the
   * repository.
   */
  public record Mq(
      String host,
      int port,
      String channel,
      String queueManager,
      String user,
      String password,
      String sendQueue,
      String replyQueue) {}
}
