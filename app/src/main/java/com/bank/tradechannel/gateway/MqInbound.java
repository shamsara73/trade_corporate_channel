package com.bank.tradechannel.gateway;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * Listens to TI's outgoing queue(s) for its replies and outward messages. Sessions are transacted: a
 * message leaves the queue only once it is stored and applied, so nothing is lost if the database is
 * down (the message comes back; TI's backout threshold stops endless redelivery).
 */
public final class MqInbound implements SmartLifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(MqInbound.class);

  private final List<DefaultMessageListenerContainer> containers = new ArrayList<>();

  public MqInbound(ConnectionFactory connectionFactory, Set<String> queues, InboundRouter router) {
    for (String queue : queues) {
      var container = new DefaultMessageListenerContainer();
      container.setConnectionFactory(connectionFactory);
      container.setDestinationName("queue:///" + queue);
      container.setSessionTransacted(true);
      container.setMessageListener(
          (jakarta.jms.MessageListener)
              message -> {
                try {
                  String text =
                      message instanceof TextMessage t ? t.getText() : message.getBody(String.class);
                  router.accept(text, "mq:" + queue);
                } catch (JMSException e) {
                  throw new IllegalStateException("cannot read a message from " + queue, e);
                }
              });
      container.afterPropertiesSet();
      containers.add(container);
      LOG.info("listening to TI on {}", queue);
    }
  }

  @Override
  public void start() {
    containers.forEach(DefaultMessageListenerContainer::start);
  }

  @Override
  public void stop() {
    containers.forEach(DefaultMessageListenerContainer::stop);
    containers.forEach(DefaultMessageListenerContainer::shutdown);
  }

  @Override
  public boolean isRunning() {
    return containers.stream().anyMatch(DefaultMessageListenerContainer::isRunning);
  }
}
