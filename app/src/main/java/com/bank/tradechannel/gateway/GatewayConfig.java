package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.TradeGateway;
import com.ibm.msg.client.jakarta.jms.JmsConnectionFactory;
import com.ibm.msg.client.jakarta.jms.JmsFactoryFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.JMSException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chooses the gateway: a file in development, IBM MQ when {@code channel.gateway.mode=mq}. */
@Configuration
class GatewayConfig {

  @Bean
  TffinnewWriter tffinnewWriter(GatewayProperties properties) {
    return new TffinnewWriter(properties);
  }

  @Bean
  @ConditionalOnProperty(name = "channel.gateway.mode", havingValue = "file", matchIfMissing = true)
  TradeGateway fileTradeGateway(TffinnewWriter writer, GatewayProperties properties) {
    return new FileTradeGateway(writer, properties.fileDirectory());
  }

  @Bean
  @ConditionalOnProperty(name = "channel.gateway.mode", havingValue = "mq")
  TradeGateway mqTradeGateway(TffinnewWriter writer, GatewayProperties properties)
      throws JMSException {
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
    return new MqTradeGateway(writer, factory, mq);
  }
}
