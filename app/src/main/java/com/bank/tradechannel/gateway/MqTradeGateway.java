package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.OutboundMessage;
import com.bank.tradechannel.domain.finance.TradeGateway;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Puts the message on TI's incoming queue as plain text (no JMS RFH2 header), as the bank's manual
 * test did, with the reply queue as JMS reply-to so TI's ServiceResponse comes back to us.
 */
public final class MqTradeGateway implements TradeGateway {
  private static final Logger LOG = LoggerFactory.getLogger(MqTradeGateway.class);

  private final TiMessageWriter writer;
  private final ConnectionFactory connectionFactory;
  private final GatewayProperties.Mq mq;

  public MqTradeGateway(
      TiMessageWriter writer, ConnectionFactory connectionFactory, GatewayProperties.Mq mq) {
    this.writer = writer;
    this.connectionFactory = connectionFactory;
    this.mq = mq;
  }

  @Override
  public SendReceipt send(OutboundMessage message) {
    String xml = writer.write(message);
    try (JMSContext context = connectionFactory.createContext()) {
      Queue queue =
          context.createQueue(
              "queue:///" + mq.sendQueue() + "?targetClient=" + WMQConstants.WMQ_CLIENT_NONJMS_MQ);
      var producer = context.createProducer();
      if (mq.replyQueue() != null && !mq.replyQueue().isBlank()) {
        producer.setJMSReplyTo(context.createQueue("queue:///" + mq.replyQueue()));
      }
      producer.send(queue, xml);
    } catch (JMSRuntimeException e) {
      throw new GatewayUnavailableException("cannot put the message on " + mq.sendQueue(), e);
    }
    LOG.info(
        "message sent to {}, eBankMasterRef={} eBankEventRef={} TheirRef={}",
        mq.sendQueue(),
        message.references().eBankMasterRef(),
        message.references().eBankEventRef(),
        message.references().theirRef());
    return new SendReceipt(mq.queueManager() + "/" + mq.sendQueue(), xml);
  }
}
