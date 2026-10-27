package com.bank.tradechannel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.tradechannel.Samples;
import com.bank.tradechannel.domain.txn.TiInbound;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * TI's messages read as the domain expects: the bank's (anonymised) and the ones built from TI
 * mappings (docs/samples/README.md says which is which).
 */
class TiMessageParserTest {
  final TiMessageParser parser = new TiMessageParser();

  static String fixture(String path) {
    return Samples.read("finance-standalone/" + path);
  }

  @Test
  void readsTheServiceResponse() {
    var reply = (TiInbound.Reply) parser.parse(fixture("create/2-ti-reply-ServiceResponse.xml"));
    assertThat(reply.succeeded()).isTrue();
    assertThat(reply.operation()).isEqualTo("TFFINNEW");
    assertThat(reply.correlationId()).isEqualTo("MSG-20261023093000-0A1B2C3D");
    assertThat(reply.details()).singleElement().asString().startsWith("Warning: ");
  }

  @Test
  void readsTheOutwardAcknowledgement() {
    var ack = (TiInbound.Outward) parser.parse(fixture("create/3-ti-response-TFFINACK.xml"));
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
  void readsTheRefusalBuiltFromTheBanksMapping() {
    var rej = (TiInbound.Outward) parser.parse(fixture("create/3-ti-response-TFFINREJ.xml"));
    assertThat(rej.kind()).isEqualTo(TiInbound.Kind.REJ);
    assertThat(rej.eBankMasterRef()).contains("TF2610230930001A");
    assertThat(rej.eBankEventRef()).contains("26102309300077");
    assertThat(rej.bankComment()).hasValueSatisfying(c -> assertThat(c).startsWith("We are unable"));
  }

  @Test
  void readsTheProposedRepaymentResponses_Q38() {
    var ack = (TiInbound.Outward) parser.parse(fixture("repay/3-ti-response-TFFRPACK.xml"));
    assertThat(ack.kind()).isEqualTo(TiInbound.Kind.ACK);
    assertThat(ack.eBankEventRef()).contains("26102409300088");
    assertThat(ack.tiEventRef()).contains("RPY001");
    assertThat(ack.outstandingMinor()).contains("5200");
    assertThat(ack.outstandingCurrency()).contains("USD");
    assertThat(ack.attachments()).hasSize(1);
    var rej = (TiInbound.Outward) parser.parse(fixture("repay/3-ti-response-TFFRPREJ.xml"));
    assertThat(rej.kind()).isEqualTo(TiInbound.Kind.REJ);
    assertThat(rej.eBankEventRef()).contains("26102409300088");
    var reply = (TiInbound.Reply) parser.parse(fixture("repay/2-ti-reply-ServiceResponse.xml"));
    assertThat(reply.correlationId()).isEqualTo("MSG-20261024093000-0A1B2C3E");
  }

  @Test
  void nilFieldsAreAbsent() {
    String text =
        fixture("create/3-ti-response-TFFINACK.xml").replace("<BankComment>We accept this financing request</BankComment>",
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
