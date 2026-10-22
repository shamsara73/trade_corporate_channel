package poc.channel;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * What the POC needs from a TFUTICRT, TFUTIACK or TFUTIREJ.
 *
 * <p>The SDK Systems Interfacing Guide says outward gateway messages are a ServiceRequest whose header
 * has Service GATEWAY and Operation DOCS or the message name. ASSUMPTION(POC-2): inside it, each mapping
 * field is an element named as in the mapping (docs/outbound/TFUTIACK.md), at any depth and in any
 * namespace. Open question Q21.
 *
 * <p>ASSUMPTION(POC-1): our TheirReference comes back as {@code InstructingPartyRef} (event field
 * IRIR, "instructing party reference"). A {@code TheirReference} element is used if present.
 */
public record OutwardMessage(
    String name,
    String theirReference,
    String eBankMasterRef,
    String eBankEventRef,
    String masterRef,
    String eventRef) {

  public static OutwardMessage parse(String xml) {
    Document doc = read(xml);
    String root = doc.getDocumentElement().getLocalName();
    String name =
        text(doc, "MessageName")
            .or(() -> text(doc, "Operation").filter(op -> !op.equals("DOCS")))
            .orElse(root);
    String their =
        text(doc, "TheirReference").or(() -> text(doc, "InstructingPartyRef")).orElse("");
    String ebankMaster = text(doc, "eBankMasterRef").orElse("");
    if (their.isEmpty() && ebankMaster.isEmpty()) {
      throw new IllegalArgumentException(
          name + " has neither InstructingPartyRef/TheirReference nor eBankMasterRef");
    }
    return new OutwardMessage(
        name,
        their,
        ebankMaster,
        text(doc, "eBankEventRef").orElse(""),
        text(doc, "MasterRef").orElse(""),
        text(doc, "EventRef").orElse(""));
  }

  private static Document read(String xml) {
    try {
      DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
      f.setNamespaceAware(true);
      f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      var builder = f.newDocumentBuilder();
      builder.setErrorHandler(null); // the exception below carries the message; no stderr noise
      return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalArgumentException("not well-formed XML: " + e.getMessage(), e);
    }
  }

  private static Optional<String> text(Document doc, String localName) {
    NodeList nodes = doc.getElementsByTagNameNS("*", localName);
    for (int i = 0; i < nodes.getLength(); i++) {
      String value = nodes.item(i).getTextContent().trim();
      if (!value.isEmpty()) {
        return Optional.of(value);
      }
    }
    return Optional.empty();
  }
}
