package poc.channel;

import com.ibm.msg.client.jakarta.jms.JmsConnectionFactory;
import com.ibm.msg.client.jakarta.jms.JmsFactoryFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** IBM MQ over JMS (client mode). One connection; one session per listener, one per send. */
public final class MqBus implements MessageBus {
  private final Config config;
  private final Connection connection;
  private final List<Session> listenerSessions = new ArrayList<>();

  public MqBus(Config config) {
    this.config = config;
    try {
      JmsConnectionFactory factory = JmsFactoryFactory.getInstance(WMQConstants.JAKARTA_WMQ_PROVIDER).createConnectionFactory();
      factory.setStringProperty(WMQConstants.WMQ_HOST_NAME, config.host());
      factory.setIntProperty(WMQConstants.WMQ_PORT, config.port());
      factory.setStringProperty(WMQConstants.WMQ_CHANNEL, config.channel());
      factory.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      factory.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, config.queueManager());
      factory.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "trade-channel-poc");
      if (!config.password().isEmpty()) {
        factory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
        factory.setStringProperty(WMQConstants.USERID, config.user());
        factory.setStringProperty(WMQConstants.PASSWORD, config.password());
      }
      this.connection = factory.createConnection();
      this.connection.start();
    } catch (JMSException e) {
      throw new IllegalStateException("cannot connect to IBM MQ: " + e.getMessage(), e);
    }
  }

  @Override
  public void send(String queue, String text) {
    try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      Destination destination = session.createQueue("queue:///" + queue);
      if (!config.rfh2()) {
        // ASSUMPTION(POC-4): TI's MQ reader wants the bare text, so no RFH2 header. Open question Q21.
        ((com.ibm.mq.jakarta.jms.MQDestination) destination).setTargetClient(WMQConstants.WMQ_CLIENT_NONJMS_MQ);
      }
      try (MessageProducer producer = session.createProducer(destination)) {
        TextMessage message = session.createTextMessage(text);
        producer.send(message);
      }
    } catch (JMSException e) {
      throw new IllegalStateException("cannot send to " + queue + ": " + e.getMessage(), e);
    }
  }

  @Override
  public void listen(String queue, Consumer<String> handler) {
    try {
      // Transacted: the message leaves the queue only after the handler returns. If the handler
      // throws (for example the JSON file cannot be written) the message goes back to the queue.
      Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
      listenerSessions.add(session);
      MessageConsumer consumer = session.createConsumer(session.createQueue("queue:///" + queue));
      consumer.setMessageListener(message -> deliver(session, message, handler));
    } catch (JMSException e) {
      throw new IllegalStateException("cannot listen on " + queue + ": " + e.getMessage(), e);
    }
  }

  private static void deliver(Session session, Message message, Consumer<String> handler) {
    try {
      if (!(message instanceof TextMessage text)) {
        handler.accept("<unsupported>" + message.getClass().getSimpleName() + "</unsupported>");
      } else {
        handler.accept(text.getText());
      }
      session.commit();
    } catch (Exception e) {
      System.err.println("handler failed, message returned to the queue: " + e);
      try {
        session.rollback();
      } catch (JMSException rollback) {
        System.err.println("rollback failed: " + rollback);
      }
    }
  }

  @Override
  public void close() {
    try {
      connection.close();
    } catch (JMSException e) {
      System.err.println("close failed: " + e.getMessage());
    }
  }
}
