package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.TiInboundProcessor;
import com.bank.tradechannel.domain.txn.InboundStore.Outcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Turns a text from TI into a domain message and hands it to the processor; shared by MQ and files. */
public final class InboundRouter {
  private static final Logger LOG = LoggerFactory.getLogger(InboundRouter.class);

  private final TiMessageParser parser;
  private final TiInboundProcessor processor;

  public InboundRouter(TiMessageParser parser, TiInboundProcessor processor) {
    this.parser = parser;
    this.processor = processor;
  }

  public Outcome accept(String text, String source) {
    Outcome outcome = processor.handle(parser.parse(text), source);
    LOG.info("message from TI via {}: {}", source, outcome);
    return outcome;
  }
}
