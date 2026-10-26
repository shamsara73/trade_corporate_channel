package com.bank.tradechannel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.tradechannel.domain.txn.TiInbound;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** TI's messages from the bank's test (anonymised fixtures) read as the domain expects. */
class TiMessageParserTest {
  final TiMessageParser parser = new TiMessageParser();

  static String fixture(String name) throws IOException {
    try (InputStream in = TiMessageParserTest.class.getResourceAsStream("/ti/" + name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void readsTheServiceResponse() throws Exception {
    var reply = (TiInbound.Reply) parser.parse(fixture("ServiceResponse-TFFINNEW.xml"));
    assertThat(reply.succeeded()).isTrue();
    assertThat(reply.operation()).isEqualTo("TFFINNEW");
    assertThat(reply.correlationId()).isEqualTo("MSG-20261023093000-0A1B2C3D");
    assertThat(reply.details()).singleElement().asString().startsWith("Warning: ");
  }

  @Test
  void readsTheOutwardAcknowledgement() throws Exception {
    var ack = (TiInbound.Outward) parser.parse(fixture("TFFINACK.xml"));
    assertThat(ack.operation()).isEqualTo("TFFINACK");
    assertThat(ack.messageName()).isEqualTo("TFFSAACK");
    assertThat(ack.kind()).isEqualTo(TiInbound.Kind.ACK);
    assertThat(ack.eBankMasterRef()).contains("TF2610230930001A");
    assertThat(ack.eBankEventRef()).contains("26102309300077");
    assertThat(ack.tiMasterRef()).contains("C001PSF0000001ID");
    assertThat(ack.tiEventRef()).contains("CRE001");
    assertThat(ack.bankComment()).contains("We accept this financing request");
    assertThat(ack.amountMinor()).contains("10200"); // Q35: minor units
    assertThat(ack.amountCurrency()).contains("USD");
    assertThat(ack.outstandingMinor()).contains("10200");
    assertThat(ack.maturityDate()).contains(LocalDate.parse("2025-07-31"));
    assertThat(ack.attachments()).singleElement().satisfies(a -> {
      assertThat(a.documentId()).isEqualTo("OCFCCREDR");
      assertThat(a.description()).isEqualTo("Debit/Credit Advice");
      assertThat(a.mimeType()).isEqualTo("application/pdf");
      assertThat(new String(a.content(), StandardCharsets.US_ASCII)).startsWith("%PDF");
    });
  }

  @Test
  void nilFieldsAreAbsent() throws Exception {
    String text =
        fixture("TFFINACK.xml").replace("<BankComment>We accept this financing request</BankComment>",
            "<BankComment xsi:nil=\"true\"/>");
    assertThat(((TiInbound.Outward) parser.parse(text)).bankComment()).isEmpty();
  }

  @Test
  void kindFromTheLastThreeLetters_Q39() {
    assertThat(TiMessageParser.kind("TFFSAREJ", "TFFINACK")).isEqualTo(TiInbound.Kind.REJ);
    assertThat(TiMessageParser.kind("TFUTICRT", "TFUTICRT")).isEqualTo(TiInbound.Kind.CRT);
    assertThat(TiMessageParser.kind("SOMETHING", "TFFINACK")).isEqualTo(TiInbound.Kind.ACK);
    assertThat(TiMessageParser.kind("X", "Y")).isEqualTo(TiInbound.Kind.OTHER);
  }

  @Test
  void keepsWhatItCannotRead() {
    assertThat(parser.parse("not xml")).isInstanceOf(TiInbound.Unreadable.class);
    assertThat(parser.parse("<a/>")).isInstanceOf(TiInbound.Unreadable.class);
    assertThat(parser.parse("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>"))
        .isInstanceOf(TiInbound.Unreadable.class);
  }
}
