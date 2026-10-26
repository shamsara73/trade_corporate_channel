package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.OutboundMessage;
import com.bank.tradechannel.domain.finance.TradeGateway;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development stand-in for TI's queue: writes each message to a file named after its message
 * reference, so the app can be tried without MQ and the output compared with the bank's sample.
 */
public class FileTradeGateway implements TradeGateway {
  private static final Logger LOG = LoggerFactory.getLogger(FileTradeGateway.class);

  private final TiMessageWriter writer;
  private final Path directory;

  public FileTradeGateway(TiMessageWriter writer, Path directory) {
    this.writer = writer;
    this.directory = directory;
  }

  @Override
  public SendReceipt send(OutboundMessage message) {
    String xml = writer.write(message);
    Path file = directory.resolve(message.references().theirRef() + ".xml");
    try {
      Files.createDirectories(directory);
      Files.writeString(file, xml, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GatewayUnavailableException("cannot write " + file, e);
    }
    // References only: the message itself holds customer data (ground rule 9).
    LOG.info(
        "message written, eBankMasterRef={} eBankEventRef={} TheirRef={}",
        message.references().eBankMasterRef(),
        message.references().eBankEventRef(),
        message.references().theirRef());
    return new SendReceipt(file.toAbsolutePath().toString(), xml);
  }
}
