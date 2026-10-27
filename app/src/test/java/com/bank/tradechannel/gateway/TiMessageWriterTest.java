package com.bank.tradechannel.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bank.tradechannel.Samples;
import com.bank.tradechannel.domain.finance.FinanceRequest;
import com.bank.tradechannel.domain.finance.FinanceSubmission;
import com.bank.tradechannel.domain.finance.InterestFrom;
import com.bank.tradechannel.domain.finance.Money;
import com.bank.tradechannel.domain.finance.RepaySubmission;
import com.bank.tradechannel.domain.finance.RepaymentAction;
import com.bank.tradechannel.domain.finance.RepaymentPayer;
import com.bank.tradechannel.domain.finance.RepaymentRequest;
import com.bank.tradechannel.domain.txn.ChannelRefs;
import com.bank.tradechannel.domain.txn.Customer;
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

/**
 * The written messages are valid for TI's XSDs; TFFINNEW also has the structure of the message TI
 * accepted. TFFSARPY has no accepted sample yet (ASSUMPTION(Q38)).
 */
class TiMessageWriterTest {
  static final Path SOURCES = Path.of("../sources");
  @TempDir static Path schemas;

  static final GatewayProperties PROPERTIES =
      new GatewayProperties(
          GatewayProperties.Mode.FILE,
          "ZONE1",
          "TRADECHANNEL",
          "CHANNEL_SVC",
          Path.of("x"),
          Path.of("y"),
          null,
          null);

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
        new ChannelRefs("TF2610260930001A", "26102609300077", "MSG-20261026093000-0A1B2C3D"),
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

  final String xml = new TiMessageWriter(PROPERTIES).write(submission("Description of Goods"));

  static RepaySubmission repayment(String interest) {
    var usd = Currency.getInstance("USD");
    return new RepaySubmission(
        new RepaymentRequest(
            "RPY-2026-1",
            RepaymentAction.INTEREST_THEN_PRINCIPAL,
            LocalDate.parse("2026-11-20"),
            RepaymentPayer.DEBIT_PARTY,
            new Money(new BigDecimal("50.00"), usd),
            new BigDecimal(interest)),
        new Customer("0009990001", "POC TEST CUSTOMER", "001"),
        "C001PSF0000001ID",
        new ChannelRefs("TF2610260930001A", "26112009300088", "MSG-20261120093000-0A1B2C3E"),
        Instant.parse("2026-11-20T09:30:00Z"));
  }

  static void assertValid(String message) {
    assertThatCode(
            () -> {
              var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
              factory
                  .newSchema(schemas.resolve("all.xsd").toFile())
                  .newValidator()
                  .validate(new StreamSource(new StringReader(message)));
            })
        .doesNotThrowAnyException();
  }

  @Test
  void isValidAgainstTheXsdJar() {
    assertValid(xml);
  }

  @Test
  void repaymentIsValidAgainstTheXsdJar_Q38() {
    assertValid(new TiMessageWriter(PROPERTIES).write(repayment("1.25")));
    assertValid(new TiMessageWriter(PROPERTIES).write(repayment("0")));
  }

  @Test
  void repaymentCarriesTheValuesAndTiMasterReference() {
    String out = new TiMessageWriter(PROPERTIES).write(repayment("1.25"));
    assertThat(out)
        .contains("<Operation>TFFSARPY</Operation>")
        .contains("<CorrelationId>MSG-20261120093000-0A1B2C3E</CorrelationId>")
        .contains("<m:MasterRef>C001PSF0000001ID</m:MasterRef>")
        .contains("<m:EBankMasterRef>TF2610260930001A</m:EBankMasterRef>")
        .contains("<m:EBankEventRef>26112009300088</m:EBankEventRef>")
        .contains("<m:RepaymentAction>I</m:RepaymentAction>")
        .contains("<m:RepaymentPayer>D</m:RepaymentPayer>")
        .contains("<m:ValueDate>2026-11-20</m:ValueDate>")
        .contains("<m:PrincipalRepaymentAmount><c:Amount>50.00</c:Amount>")
        .contains("<m:InterestRepaymentAmount><c:Amount>1.25</c:Amount>")
        .contains("<m:TotalRepaymentAmount><c:Amount>51.25</c:Amount>");
    assertThat(new TiMessageWriter(PROPERTIES).write(repayment("0")))
        .doesNotContain("InterestRepaymentAmount");
  }

  @Test
  void hasTheSameStructureAsTheMessageTiAccepted() throws Exception {
    String accepted = Samples.read(CREATE_REQUEST);
    assertThat(paths(xml)).containsExactlyElementsOf(paths(accepted));
  }

  static final String CREATE_REQUEST = "finance-standalone/create/1-tcc-request-TFFINNEW.xml";
  static final String REPAY_REQUEST = "finance-standalone/repay/1-tcc-request-TFFSARPY.xml";

  /** The repayment of docs/samples: same customer and references as the CREATE sample. */
  static RepaySubmission repaySample() {
    var usd = Currency.getInstance("USD");
    return new RepaySubmission(
        new RepaymentRequest(
            "RPY-2025-1",
            RepaymentAction.INTEREST_THEN_PRINCIPAL,
            LocalDate.parse("2025-07-01"),
            RepaymentPayer.DEBIT_PARTY,
            new Money(new BigDecimal("50.00"), usd),
            new BigDecimal("1.25")),
        new Customer("0009990001", "POC TEST CUSTOMER", "001"),
        "C001PSF0000001ID",
        new ChannelRefs("TF2610230930001A", "26102409300088", "MSG-20261024093000-0A1B2C3E"),
        Instant.parse("2025-07-01T09:30:00Z"));
  }

  /**
   * The documented TFFSARPY sample is what the channel writes (same header values as the accepted
   * TFFINNEW). Run with -Dsamples.update=true to rewrite it after a deliberate change.
   */
  @Test
  void repaymentSampleIsWhatTheChannelWrites() throws IOException {
    var header =
        new GatewayProperties(
            GatewayProperties.Mode.FILE, "ZONE1", "TRADECHANNEL", "SUPERVISOR", Path.of("x"),
            Path.of("y"), null, null);
    String written = new TiMessageWriter(header).write(repaySample());
    Path sample = Samples.DIR.resolve(REPAY_REQUEST);
    if (Boolean.getBoolean("samples.update")) {
      Files.writeString(sample, indented(written), StandardCharsets.UTF_8);
    }
    assertThat(compact(Samples.read(REPAY_REQUEST))).isEqualTo(compact(written));
    assertValid(written);
  }

  /** The sample is stored indented for reading; whitespace between tags is not part of it. */
  static String compact(String xml) {
    return xml.replaceAll(">\\s+<", "><").strip();
  }

  static String indented(String xml) {
    try {
      var transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      var out = new java.io.StringWriter();
      transformer.transform(
          new StreamSource(new StringReader(xml)), new javax.xml.transform.stream.StreamResult(out));
      return out.toString().replace("?><", "?>\n<").strip() + "\n";
    } catch (javax.xml.transform.TransformerException e) {
      throw new IllegalStateException(e);
    }
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
    String out = new TiMessageWriter(PROPERTIES).write(plain);
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
