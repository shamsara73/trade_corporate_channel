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
  void sampleHasUniqueReferencesWithinTheirLimit() {
    SampleMessage samples = new SampleMessage(clock);
    SampleMessage.Built a = samples.build();
    SampleMessage.Built b = samples.build();
    assertTrue(a.theirReference().length() <= 34);
    assertTrue(!a.theirReference().equals(b.theirReference()));
    assertTrue(a.xml().contains("<c:TheirReference>" + a.theirReference() + "</c:TheirReference>"));
    assertTrue(!a.xml().contains("${"));
    // well-formed
    javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(a.xml().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
  }

  @Test
  void ackMarksSentReferenceProcessed() {
    store.recordSent("POC-1", "CH-1", "CH-1-01");
    router.handle(Fixtures.outward("TFUTIACK", "POC-1", "CH-1", "ack"));
    assertEquals(Status.PROCESSED, only("POC-1").status());
    assertEquals("IGT000123", only("POC-1").masterRef());
    assertEquals(Origin.CHANNEL, only("POC-1").origin());
  }

  @Test
  void rejMarksSentReferenceRejected() {
    store.recordSent("POC-2", "CH-2", "CH-2-01");
    router.handle(Fixtures.outward("TFUTIREJ", "POC-2", "CH-2", "no"));
    assertEquals(Status.REJECTED, only("POC-2").status());
  }

  @Test
  void matchesByEBankMasterRefWhenTheirReferenceIsAbsent() {
    store.recordSent("POC-3", "CH-3", "CH-3-01");
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
    store.recordSent("POC-5", "CH-5", "CH-5-01");
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
    store.recordSent("POC-6", "CH-6", "CH-6-01");
    router.handle(Fixtures.outward("TFUTIACK", "POC-6", "CH-6", "ack"));
    ReferenceStore reopened = new ReferenceStore(dir.resolve("references.json"), clock);
    assertEquals(Status.PROCESSED, reopened.all().get(0).status());
    assertTrue(Files.readString(dir.resolve("references.json")).contains("\"status\" : \"PROCESSED\""));
  }
}
