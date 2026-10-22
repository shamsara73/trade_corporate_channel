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
 * TI's reply to a TFIGTAPP (ServiceResponse) may arrive on a queue this app listens to. It is not an
 * outward message, so it is logged rather than parked: operation, status and any error text.
 */
final class ServiceResponseSummary {
  private ServiceResponseSummary() {}

  static Optional<String> of(String xml) {
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
    List<String> parts = new ArrayList<>();
    for (String name : List.of("Operation", "Status", "CorrelationId")) {
      parts.add(name + "=" + first(doc, name));
    }
    for (String name : List.of("Error", "Warning", "Info")) {
      NodeList nodes = doc.getElementsByTagNameNS("*", name);
      for (int i = 0; i < nodes.getLength(); i++) {
        parts.add(name + ": " + nodes.item(i).getTextContent().trim());
      }
    }
    return Optional.of(String.join(", ", parts));
  }

  private static String first(Document doc, String name) {
    NodeList nodes = doc.getElementsByTagNameNS("*", name);
    return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
  }
}
