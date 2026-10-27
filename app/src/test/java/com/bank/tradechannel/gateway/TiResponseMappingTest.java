package com.bank.tradechannel.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.tradechannel.Samples;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TI's outward messages are configured in TI as mappings ({@code FieldName<<TAG,type,sub>>}); the
 * field name becomes the XML element. Each response sample has every plain field of its mapping
 * and nothing else (a table such as Charges appears only when it has rows), and an ACK or REJ
 * carries the fields the channel reads ({@link TiMessageParser}).
 */
class TiResponseMappingTest {
  private static final Pattern FIELD = Pattern.compile("(\\S+?) ?<<(.*?)>>");
  private static final Pattern ELEMENT = Pattern.compile("^<([A-Za-z]\\w*)[\\s/>]", Pattern.MULTILINE);

  /** What TiMessageParser and the transaction page use from a response. */
  static final Set<String> READ_BY_THE_CHANNEL =
      Set.of("MessageName", "eBankMasterRef", "eBankEventRef", "MasterRef", "EventRef", "BankComment");

  static final Set<String> READ_FROM_AN_ACK =
      Set.of("OutstandingAmt", "OutstandingCcy", "MaturityDate", "Status", "AttachedDocuments");

  @ParameterizedTest
  @CsvSource({
    "../sources/ti_outbound/TFFINREJ.txt, finance-standalone/create/3-ti-response-TFFINREJ.xml",
    "../docs/outbound/proposed/TFFRPACK.txt, finance-standalone/repay/3-ti-response-TFFRPACK.xml",
    "../docs/outbound/proposed/TFFRPREJ.txt, finance-standalone/repay/3-ti-response-TFFRPREJ.xml",
  })
  void sampleHasTheFieldsOfItsMapping(String mapping, String sample) throws IOException {
    Map<String, String> fields = fields(Path.of(mapping));
    Set<String> plain = new LinkedHashSet<>();
    fields.forEach(
        (name, body) -> {
          String[] parts = body.split(",");
          if (parts.length < 2 || !"*".equals(parts[1])) {
            plain.add(name);
          }
        });
    String xml = Samples.read(sample);
    Set<String> elements = payloadElements(xml);
    assertThat(elements).containsAll(plain);
    assertThat(fields.keySet()).containsAll(elements);

    String messageName = fields.get("MessageName").replaceFirst("^TEXT,T,", "");
    assertThat(xml).contains("<Operation>" + messageName + "</Operation>");
    assertThat(fields.keySet()).containsAll(READ_BY_THE_CHANNEL);
    // ASSUMPTION(Q39): the kind is read from the last three letters of the name.
    assertThat(TiMessageParser.kind(messageName, messageName))
        .isIn(com.bank.tradechannel.domain.txn.TiInbound.Kind.ACK,
            com.bank.tradechannel.domain.txn.TiInbound.Kind.REJ);
    if (messageName.endsWith("ACK")) {
      assertThat(fields.keySet()).containsAll(READ_FROM_AN_ACK);
    }
  }

  /** Field name to the text between {@code << >>}, in mapping order (repeated names kept once). */
  static Map<String, String> fields(Path mapping) throws IOException {
    Map<String, String> out = new LinkedHashMap<>();
    var m = FIELD.matcher(Files.readString(mapping, StandardCharsets.UTF_8));
    while (m.find()) {
      out.putIfAbsent(m.group(1), m.group(2));
    }
    return out;
  }

  /** The elements of the payload: one per line in TI's outward messages. */
  static Set<String> payloadElements(String xml) {
    String payload = xml.substring(xml.indexOf("</RequestHeader>") + "</RequestHeader>".length());
    payload = payload.substring(payload.indexOf('\n') + 1);
    Set<String> out = new LinkedHashSet<>();
    var m = ELEMENT.matcher(payload);
    while (m.find()) {
      out.add(m.group(1));
    }
    return out;
  }
}
