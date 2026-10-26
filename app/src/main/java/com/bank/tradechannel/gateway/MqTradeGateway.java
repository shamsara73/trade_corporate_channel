package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.FinanceSubmission;
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
 * test did. TI answers with a ServiceResponse on the reply queue; reading it arrives with task T9.
 */
public final class MqTradeGateway implements TradeGateway {
  private static final Logger LOG = LoggerFactory.getLogger(MqTradeGateway.class);

  private final TffinnewWriter writer;
  private final ConnectionFactory connectionFactory;
  private final GatewayProperties.Mq mq;

  public MqTradeGateway(
      TffinnewWriter writer, ConnectionFactory connectionFactory, GatewayProperties.Mq mq) {
    this.writer = writer;
    this.connectionFactory = connectionFactory;
    this.mq = mq;
  }

  @Override
  public SendReceipt send(FinanceSubmission submission) {
    String xml = writer.write(submission);
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
        "TFFINNEW sent to {}, eBankMasterRef={} eBankEventRef={} TheirRef={}",
        mq.sendQueue(),
        submission.references().eBankMasterRef(),
        submission.references().eBankEventRef(),
        submission.references().theirRef());
    return new SendReceipt(mq.queueManager() + "/" + mq.sendQueue());
  }
}
