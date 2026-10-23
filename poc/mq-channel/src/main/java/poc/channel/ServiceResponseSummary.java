package poc.channel;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * TI's immediate reply to a sent message. With ReplyFormat FULL it comes straight back, echoing our
 * CorrelationId; SUCCEEDED means TI accepted the message and created the transaction (seen in the
 * bank's test, 2026-10-23), FAILED carries the errors.
 */
record ServiceResponseSummary(String operation, String status, String correlationId, List<String> details) {

  static Optional<ServiceResponseSummary> of(String xml) {
    Document doc;
    try {
      DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
      f.setNamespaceAware(true);
      f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      var builder = f.newDocumentBuilder();
      builder.setErrorHandler(null);
      doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      return Optional.empty();
    }
    if (!"ServiceResponse".equals(doc.getDocumentElement().getLocalName())) {
      return Optional.empty();
    }
    List<String> details = new ArrayList<>();
    for (String name : List.of("Error", "Warning", "Info")) {
      NodeList nodes = doc.getElementsByTagNameNS("*", name);
      for (int i = 0; i < nodes.getLength(); i++) {
        details.add(name + ": " + nodes.item(i).getTextContent().trim());
      }
    }
    return Optional.of(new ServiceResponseSummary(first(doc, "Operation"), first(doc, "Status"), first(doc, "CorrelationId"), details));
  }

  @Override
  public String toString() {
    List<String> parts = new ArrayList<>(List.of("Operation=" + operation, "Status=" + status, "CorrelationId=" + correlationId));
    parts.addAll(details);
    return String.join(", ", parts);
  }

  private static String first(Document doc, String name) {
    NodeList nodes = doc.getElementsByTagNameNS("*", name);
    return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
  }
}
