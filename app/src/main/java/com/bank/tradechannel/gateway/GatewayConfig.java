package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.TiInboundProcessor;
import com.bank.tradechannel.domain.finance.TradeGateway;
import com.ibm.msg.client.jakarta.jms.JmsConnectionFactory;
import com.ibm.msg.client.jakarta.jms.JmsFactoryFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Chooses the transport: IBM MQ when {@code channel.gateway.mode=mq}; files otherwise, optionally
 * answered by the simulated TI ({@code channel.gateway.simulator.enabled=true}).
 */
@Configuration
class GatewayConfig {

  @Bean
  TiMessageWriter tiMessageWriter(GatewayProperties properties) {
    return new TiMessageWriter(properties);
  }

  @Bean
  InboundRouter inboundRouter(TiInboundProcessor processor) {
    return new InboundRouter(new TiMessageParser(), processor);
  }

  @Configuration
  @ConditionalOnProperty(name = "channel.gateway.mode", havingValue = "file", matchIfMissing = true)
  static class FileTransport {

    @Bean
    TradeGateway tradeGateway(TiMessageWriter writer, GatewayProperties properties, Clock clock) {
      var files = new FileTradeGateway(writer, properties.fileDirectory());
      if (properties.simulator().enabled()) {
        return new SimulatedTi(
            files, properties.inboxDirectory(), properties.simulator().completeAfter(), clock);
      }
      return files;
    }

    @Bean
    FileInbox fileInbox(GatewayProperties properties, InboundRouter router) {
      return new FileInbox(properties.inboxDirectory(), router);
    }

    @Bean
    @ConditionalOnProperty(name = "channel.jobs.enabled", havingValue = "true", matchIfMissing = true)
    FileInboxJob fileInboxJob(FileInbox inbox) {
      return new FileInboxJob(inbox);
    }
  }

  /** Polls the file inbox; MQ needs no polling, its listener is pushed messages. */
  static final class FileInboxJob {
    private final FileInbox inbox;

    FileInboxJob(FileInbox inbox) {
      this.inbox = inbox;
    }

    @Scheduled(fixedDelayString = "${channel.jobs.poll-millis:2000}")
    void poll() {
      inbox.poll();
    }
  }

  @Configuration
  @ConditionalOnProperty(name = "channel.gateway.mode", havingValue = "mq")
  static class MqTransport {

    @Bean
    ConnectionFactory tiConnectionFactory(GatewayProperties properties) throws JMSException {
      var mq = properties.mq();
      JmsConnectionFactory factory =
          JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER).createConnectionFactory();
      factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, mq.host());
      factory.setIntProperty(WMQConstants.WMQ_PORT, mq.port());
      factory.setStringProperty(WMQConstants.WMQ_CHANNEL, mq.channel());
      factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, mq.queueManager());
      factory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "trade-channel");
      if (mq.password() != null && !mq.password().isBlank()) {
        factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        factory.setStringProperty(WMQConstants.USERID, mq.user());
        factory.setStringProperty(WMQConstants.PASSWORD, mq.password());
      }
      return factory;
    }

    @Bean
    TradeGateway tradeGateway(
        TiMessageWriter writer, ConnectionFactory tiConnectionFactory, GatewayProperties properties) {
      return new MqTradeGateway(writer, tiConnectionFactory, properties.mq());
    }

    @Bean
    @ConditionalOnProperty(name = "channel.jobs.enabled", havingValue = "true", matchIfMissing = true)
    MqInbound mqInbound(
        ConnectionFactory tiConnectionFactory, GatewayProperties properties, InboundRouter router) {
      return new MqInbound(tiConnectionFactory, properties.mq().listenQueues(), router);
    }
  }
}
