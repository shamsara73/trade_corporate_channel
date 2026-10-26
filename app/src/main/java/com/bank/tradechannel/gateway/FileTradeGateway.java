package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.FinanceSubmission;
import com.bank.tradechannel.domain.finance.TradeGateway;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development stand-in for TI's queue: writes each message to a file named after its message
 * reference, so the form can be tried without MQ and the output compared with the bank's sample.
 */
public final class FileTradeGateway implements TradeGateway {
  private static final Logger LOG = LoggerFactory.getLogger(FileTradeGateway.class);

  private final TffinnewWriter writer;
  private final Path directory;

  public FileTradeGateway(TffinnewWriter writer, Path directory) {
    this.writer = writer;
    this.directory = directory;
  }

  @Override
  public SendReceipt send(FinanceSubmission submission) {
    Path file = directory.resolve(submission.references().theirRef() + ".xml");
    try {
      Files.createDirectories(directory);
      Files.writeString(file, writer.write(submission), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GatewayUnavailableException("cannot write " + file, e);
    }
    // References only: the message itself holds customer data (ground rule 9).
    LOG.info(
        "TFFINNEW written, eBankMasterRef={} eBankEventRef={} TheirRef={}",
        submission.references().eBankMasterRef(),
        submission.references().eBankEventRef(),
        submission.references().theirRef());
    return new SendReceipt(file.toAbsolutePath().toString());
  }
}
