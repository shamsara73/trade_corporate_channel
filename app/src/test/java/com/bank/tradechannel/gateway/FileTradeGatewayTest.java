package com.bank.tradechannel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTradeGatewayTest {
  @TempDir Path dir;

  @Test
  void writesTheMessageNamedByItsMessageReference() throws Exception {
    var gateway = new FileTradeGateway(new TiMessageWriter(TiMessageWriterTest.PROPERTIES), dir);
    var receipt = gateway.send(TiMessageWriterTest.submission("goods"));
    Path file = dir.resolve("MSG-20261026093000-0A1B2C3D.xml");
    assertThat(receipt.destination()).isEqualTo(file.toAbsolutePath().toString());
    assertThat(Files.readString(file)).contains("<Operation>TFFINNEW</Operation>");
    assertThat(receipt.body()).isEqualTo(Files.readString(file));
  }
}
