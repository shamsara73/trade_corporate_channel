package poc.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OutwardMessageTest {
  @Test
  void readsReferencesAtAnyDepthAndNamespace() {
    OutwardMessage m = OutwardMessage.parse(Fixtures.outward("TFUTIACK", "POC-1", "CH-1", "ok"));
    assertEquals("TFUTIACK", m.name());
    assertEquals("POC-1", m.theirReference()); // ASSUMPTION(POC-1)
    assertEquals("CH-1", m.eBankMasterRef());
    assertEquals("IGT000123", m.masterRef());
  }

  @Test
  void theirReferenceElementWinsOverInstructingPartyRef() {
    String xml = "<TFUTICRT><TheirReference>A</TheirReference><InstructingPartyRef>B</InstructingPartyRef></TFUTICRT>";
    assertEquals("A", OutwardMessage.parse(xml).theirReference());
  }

  @Test
  void rootNameIsUsedWhenMessageNameIsMissing() {
    assertEquals("TFUTIREJ", OutwardMessage.parse("<TFUTIREJ><InstructingPartyRef>X</InstructingPartyRef></TFUTIREJ>").name());
  }

  @Test
  void readsAnOutwardMessageInsideAGatewayServiceRequest() {
    String xml =
        """
        <ServiceRequest xmlns="urn:control.services.tiplus2.misys.com">
          <RequestHeader><Service>GATEWAY</Service><Operation>DOCS</Operation></RequestHeader>
          <TFUTIACK><MessageName>TFUTIACK</MessageName><InstructingPartyRef>P-1</InstructingPartyRef></TFUTIACK>
        </ServiceRequest>
        """;
    OutwardMessage m = OutwardMessage.parse(xml);
    assertEquals("TFUTIACK", m.name());
    assertEquals("P-1", m.theirReference());
  }

  @Test
  void usesTheHeaderOperationWhenRoutedByMessageName() {
    String xml =
        "<ServiceRequest><RequestHeader><Service>GATEWAY</Service><Operation>TFUTIREJ</Operation></RequestHeader>"
            + "<Payload><InstructingPartyRef>P-2</InstructingPartyRef></Payload></ServiceRequest>";
    assertEquals("TFUTIREJ", OutwardMessage.parse(xml).name());
  }

  @Test
  void rejectsMessageWithoutAnyReference() {
    assertThrows(IllegalArgumentException.class, () -> OutwardMessage.parse("<TFUTIACK><MasterRef>M</MasterRef></TFUTIACK>"));
  }

  @Test
  void rejectsDoctype() {
    assertThrows(IllegalArgumentException.class, () -> OutwardMessage.parse("<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>"));
  }

  @Test
  void rejectsNonXml() {
    assertThrows(IllegalArgumentException.class, () -> OutwardMessage.parse("hello"));
  }
}
