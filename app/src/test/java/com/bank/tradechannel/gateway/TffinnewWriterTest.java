package com.bank.tradechannel.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bank.tradechannel.domain.finance.ChannelReferences;
import com.bank.tradechannel.domain.finance.Customer;
import com.bank.tradechannel.domain.finance.FinanceRequest;
import com.bank.tradechannel.domain.finance.FinanceSubmission;
import com.bank.tradechannel.domain.finance.InterestFrom;
import com.bank.tradechannel.domain.finance.Money;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** The written message is valid for TI and has the structure of the message TI accepted. */
class TffinnewWriterTest {
  static final Path SOURCES = Path.of("../sources");
  @TempDir static Path schemas;

  static final GatewayProperties PROPERTIES =
      new GatewayProperties(
          GatewayProperties.Mode.FILE, "ZONE1", "TRADECHANNEL", "CHANNEL_SVC", Path.of("x"), null);

  static FinanceSubmission submission(String goods) {
    var usd = Currency.getInstance("USD");
    var request =
        new FinanceRequest(
            "FIN-REQ-2026-9",
            new Money(new BigDecimal("102.00"), usd),
            new Money(new BigDecimal("101.00"), usd),
            LocalDate.parse("2026-10-26"),
            60,
            false,
            InterestFrom.AFTER,
            "PSF",
            goods,
            "Please finance & book",
            "Additional information");
    return new FinanceSubmission(
        request,
        new Customer("0009990001", "POC TEST CUSTOMER", "001"),
        new ChannelReferences("TF2610260930001A", "26102609300077", "MSG-20261026093000-0A1B2C3D"),
        Instant.parse("2026-10-26T09:30:00Z"));
  }

  /** TI loads both jars as one tree; the customisation jar supplies services/custom/. */
  @BeforeAll
  static void unpackTheXsdJars() throws IOException {
    for (String jar :
        List.of(
            "com.misys.tiplus2.gen.ti-xsd.jar", "com.misys.tiplus2.gen.ti-xsd.customisation.jar")) {
      try (var zip = new ZipFile(SOURCES.resolve(jar).toFile())) {
        for (var entry : java.util.Collections.list(zip.entries())) {
          Path target = schemas.resolve(entry.getName()).normalize();
          if (!target.startsWith(schemas) || entry.isDirectory()) {
            continue;
          }
          Files.createDirectories(target.getParent());
          try (InputStream in = zip.getInputStream(entry)) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    }
    Files.writeString(
        schemas.resolve("all.xsd"),
        """
        <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" targetNamespace="urn:local.wrapper">
          <xsd:import namespace="urn:control.services.tiplus2.misys.com" schemaLocation="control/control.xsd"/>
          <xsd:import namespace="urn:messages.service.ti.apps.tiplus2.misys.com" schemaLocation="services/services.xsd"/>
        </xsd:schema>
        """);
  }

  final String xml = new TffinnewWriter(PROPERTIES).write(submission("Description of Goods"));

  @Test
  void isValidAgainstTheXsdJar() {
    assertThatCode(
            () -> {
              var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
              factory
                  .newSchema(schemas.resolve("all.xsd").toFile())
                  .newValidator()
                  .validate(new StreamSource(new StringReader(xml)));
            })
        .doesNotThrowAnyException();
  }

  @Test
  void hasTheSameStructureAsTheMessageTiAccepted() throws Exception {
    String accepted;
    try (InputStream in = getClass().getResourceAsStream("/ti/TFFINNEW-accepted.xml")) {
      accepted = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertThat(paths(xml)).containsExactlyElementsOf(paths(accepted));
  }

  @Test
  void carriesTheValues() {
    assertThat(xml)
        .contains("<Operation>TFFINNEW</Operation>")
        .contains("<CorrelationId>MSG-20261026093000-0A1B2C3D</CorrelationId>")
        .contains("<m:TheirRef>MSG-20261026093000-0A1B2C3D</m:TheirRef>")
        .contains("<c:TheirReference>FIN-REQ-2026-9</c:TheirReference>")
        .contains("<c:Reference>FIN-REQ-2026-9</c:Reference>")
        .contains(
            "<m:FinanceAmount><c:Amount>102.00</c:Amount><c:Currency>USD</c:Currency></m:FinanceAmount>")
        .contains("<m:Amount><c:Amount>101.00</c:Amount>")
        .contains("<m:MaturityDate>2026-12-25</m:MaturityDate>")
        .contains("<m:ProductCode>FSA</m:ProductCode>")
        .contains("<c:TenorDays>60</c:TenorDays>")
        .contains("Please finance &amp; book");
  }

  @Test
  void leavesOutEmptyNotes() {
    var plain =
        new FinanceSubmission(
            new FinanceRequest(
                "R",
                submission("").request().financeAmount(),
                submission("").request().financeAmount(),
                LocalDate.parse("2026-10-26"),
                60,
                true,
                InterestFrom.FROM,
                "PSF",
                "",
                "",
                ""),
            submission("").customer(),
            submission("").references(),
            Instant.EPOCH);
    String out = new TffinnewWriter(PROPERTIES).write(plain);
    assertThat(out)
        .doesNotContain("EventNotificationss")
        .contains("<m:TrustReceiptLoan>Y</m:TrustReceiptLoan>");
  }

  /** Element paths in document order, namespace prefixes ignored. */
  static List<String> paths(String xml) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    var doc =
        factory
            .newDocumentBuilder()
            .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    List<String> out = new ArrayList<>();
    walk(doc.getDocumentElement(), "", out);
    return out;
  }

  private static void walk(Element e, String parent, List<String> out) {
    String path = parent + "/" + e.getLocalName();
    out.add(path);
    for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element child) {
        walk(child, path, out);
      }
    }
  }
}
