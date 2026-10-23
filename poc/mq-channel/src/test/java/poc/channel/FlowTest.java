package poc.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The whole POC flow without MQ: the router and the JSON file. */
class FlowTest {
  @TempDir Path dir;
  Clock clock = Clock.fixed(Instant.parse("2026-10-19T10:00:00Z"), ZoneOffset.UTC);
  List<String> console = new ArrayList<>();
  ReferenceStore store;
  MessageRouter router;

  @BeforeEach
  void setUp() {
    store = new ReferenceStore(dir.resolve("references.json"), clock);
    router = new MessageRouter(store, dir.resolve("unprocessed.log"), clock, console::add);
  }

  private Reference only(String theirReference) {
    return store.all().stream().filter(r -> r.theirReference().equals(theirReference)).findFirst().orElseThrow();
  }

  @Test
  void tffinnewSampleCarriesFreshReferencesAndDates() {
    SampleMessage samples = new SampleMessage("TFFINNEW", new SampleMessage.Party("CUST01", "A & B TRADING", "001"), clock, new java.util.Random(1));
    SampleMessage.Built a = samples.build();
    SampleMessage.Built b = samples.build();
    assertEquals("TFFINNEW", a.messageName());
    assertTrue(a.theirReference().length() <= 34);
    assertEquals(14, a.eBankEventRef().length());
    assertEquals("TF" + a.eBankEventRef(), a.eBankMasterRef()); // ASSUMPTION(Q34)
    assertTrue(!a.theirReference().equals(b.theirReference()));
    // TheirRef is the message reference: unique per message delivered, and different from the customer reference.
    assertTrue(!a.messageRef().equals(b.messageRef()));
    assertTrue(a.messageRef().length() <= 35);
    assertTrue(a.xml().contains("<ns2:TheirRef>" + a.messageRef() + "</ns2:TheirRef>"));
    // The customer reference stays in Context/TheirReference and DebitParty/Reference.
    assertEquals(2, a.xml().split(java.util.regex.Pattern.quote(a.theirReference()), -1).length - 1);
    assertTrue(a.xml().contains("<ns2:StartDate>2026-10-19</ns2:StartDate>"));
    assertTrue(a.xml().contains("<ns2:MaturityDate>2026-12-18</ns2:MaturityDate>"));
    assertTrue(a.xml().contains("A &amp; B TRADING"));
    assertTrue(!a.xml().contains("${"));
    assertWellFormed(a.xml());
  }

  @Test
  void tfigtappTemplateIsStillAvailable() {
    SampleMessage.Built m = new SampleMessage("TFIGTAPP", new SampleMessage.Party("CUST01", "X", "001"), clock, new java.util.Random(2)).build();
    assertTrue(m.xml().contains("<ns2:eBankEvent>" + m.eBankEventRef() + "</ns2:eBankEvent>"));
    assertTrue(!m.xml().contains("${"));
    assertWellFormed(m.xml());
  }

  private static void assertWellFormed(String xml) {
    javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  }

  @Test
  void ackMarksSentReferenceProcessed() {
    store.recordSent("TFFINNEW", "POC-1", "MSG-POC-1", "CH-1", "CH-1-01");
    router.handle(Fixtures.outward("TFUTIACK", "POC-1", "CH-1", "ack"));
    assertEquals(Status.PROCESSED, only("POC-1").status());
    assertEquals("IGT000123", only("POC-1").masterRef());
    assertEquals(Origin.CHANNEL, only("POC-1").origin());
  }

  @Test
  void rejMarksSentReferenceRejected() {
    store.recordSent("TFFINNEW", "POC-2", "MSG-POC-2", "CH-2", "CH-2-01");
    router.handle(Fixtures.outward("TFUTIREJ", "POC-2", "CH-2", "no"));
    assertEquals(Status.REJECTED, only("POC-2").status());
  }

  @Test
  void eventNumbersPickTheRightEventOfATransaction() {
    store.recordSent("TFFINNEW", "POC-7", "MSG-1", "TF100", "100");
    store.recordSent("TFFINNEW", "POC-8", "MSG-2", "TF100", "101");
    router.handle("<TFUTIACK><eBankMasterRef>TF100</eBankMasterRef><eBankEventRef>101</eBankEventRef></TFUTIACK>");
    assertEquals(Status.SENT, only("POC-7").status());
    assertEquals(Status.PROCESSED, only("POC-8").status());
    assertEquals("MSG-2", only("POC-8").messageRef());
  }

  @Test
  void matchesByEBankMasterRefWhenTheirReferenceIsAbsent() {
    store.recordSent("TFFINNEW", "POC-3", "MSG-POC-3", "CH-3", "CH-3-01");
    router.handle("<TFUTIACK><eBankMasterRef>CH-3</eBankMasterRef></TFUTIACK>");
    assertEquals(Status.PROCESSED, only("POC-3").status());
    assertEquals(1, store.all().size());
  }

  @Test
  void crtFromTiIsSavedAndLaterAckOrRejUpdatesIt() {
    router.handle(Fixtures.outward("TFUTICRT", "TI-REF-9", "", "issued"));
    assertEquals(Status.TI_CREATED, only("TI-REF-9").status());
    assertEquals(Origin.TI, only("TI-REF-9").origin());

    router.handle(Fixtures.outward("TFUTIACK", "TI-REF-9", "", "ack"));
    assertEquals(Status.PROCESSED, only("TI-REF-9").status());
    router.handle(Fixtures.outward("TFUTIREJ", "TI-REF-9", "", "later refusal"));
    assertEquals(Status.REJECTED, only("TI-REF-9").status());
    assertEquals(Origin.TI, only("TI-REF-9").origin());
  }

  @Test
  void repeatedCrtDoesNotResetStatus() {
    router.handle(Fixtures.outward("TFUTICRT", "TI-REF-8", "", "issued"));
    router.handle(Fixtures.outward("TFUTIACK", "TI-REF-8", "", "ack"));
    router.handle(Fixtures.outward("TFUTICRT", "TI-REF-8", "", "issued"));
    assertEquals(Status.PROCESSED, only("TI-REF-8").status());
  }

  @Test
  void crtForAChannelReferenceIsLeftAlone() {
    store.recordSent("TFFINNEW", "POC-5", "MSG-POC-5", "CH-5", "CH-5-01");
    router.handle(Fixtures.outward("TFUTICRT", "POC-5", "CH-5", "issued"));
    assertEquals(Status.SENT, only("POC-5").status());
    assertEquals(Origin.CHANNEL, only("POC-5").origin());
  }

  @Test
  void ackForUnknownReferenceIsRecordedAsUnknown() {
    router.handle(Fixtures.outward("TFUTIACK", "NOPE", "", "ack"));
    assertEquals(Origin.UNKNOWN, only("NOPE").origin());
    assertEquals(Status.PROCESSED, only("NOPE").status());
  }

  @Test
  void badMessagesAreParkedNotLost() throws Exception {
    router.handle("not xml");
    router.handle("<TFUTIXXX><InstructingPartyRef>R</InstructingPartyRef></TFUTIXXX>");
    assertEquals(0, store.all().size());
    String log = Files.readString(dir.resolve("unprocessed.log"));
    assertTrue(log.contains("not xml"));
    assertTrue(log.contains("TFUTIXXX"));
  }

  @Test
  void serviceResponseIsLoggedNotParked() {
    router.handle(
        "<ServiceResponse xmlns=\"urn:control.services.tiplus2.misys.com\"><ResponseHeader><Service>TI</Service>"
            + "<Operation>TFIGTAPP</Operation><Status>FAILED</Status><Details><Error>Bad currency</Error></Details>"
            + "<CorrelationId>C-1</CorrelationId></ResponseHeader></ServiceResponse>");
    assertTrue(console.get(0).contains("Status=FAILED"));
    assertTrue(console.get(0).contains("Error: Bad currency"));
    assertTrue(!Files.exists(dir.resolve("unprocessed.log")));
  }

  @Test
  void jsonFileSurvivesRestart() throws Exception {
    store.recordSent("TFFINNEW", "POC-6", "MSG-POC-6", "CH-6", "CH-6-01");
    router.handle(Fixtures.outward("TFUTIACK", "POC-6", "CH-6", "ack"));
    ReferenceStore reopened = new ReferenceStore(dir.resolve("references.json"), clock);
    assertEquals(Status.PROCESSED, reopened.all().get(0).status());
    assertTrue(Files.readString(dir.resolve("references.json")).contains("\"status\" : \"PROCESSED\""));
  }
}
