package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.txn.TiInbound;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Reads what TI puts on the queue: a ServiceResponse (its reply to our message) or a ServiceRequest
 * with Service GATEWAY (an outward message such as TFFINACK). Outward payloads have one flat element
 * per mapping field, empty ones as {@code xsi:nil}, text possibly in CDATA, documents in base64
 * (docs/reference/working-samples.md, section 2).
 */
public final class TiMessageParser {
  private static final String XSI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

  public TiInbound parse(String text) {
    Document doc;
    try {
      doc = read(text);
    } catch (Exception e) {
      return new TiInbound.Unreadable("not well-formed XML: " + e.getMessage(), text);
    }
    Element root = doc.getDocumentElement();
    if ("ServiceResponse".equals(root.getLocalName())) {
      return reply(root, text);
    }
    if ("ServiceRequest".equals(root.getLocalName())) {
      Optional<Element> header = child(root, "RequestHeader");
      String service = header.flatMap(h -> childText(h, "Service")).orElse("");
      if ("GATEWAY".equals(service)) {
        return outward(header.get(), root, text);
      }
      return new TiInbound.Unreadable("ServiceRequest for service " + service + " is not for us", text);
    }
    return new TiInbound.Unreadable("unexpected root element " + root.getLocalName(), text);
  }

  private static TiInbound.Reply reply(Element root, String text) {
    Element header = child(root, "ResponseHeader").orElse(root);
    List<String> details = new ArrayList<>();
    child(header, "Details")
        .ifPresent(
            d -> {
              for (Element e : children(d)) {
                details.add(e.getLocalName() + ": " + e.getTextContent().trim());
              }
            });
    return new TiInbound.Reply(
        childText(header, "Operation").orElse(""),
        childText(header, "Status").orElse(""),
        childText(header, "CorrelationId").orElse(""),
        details,
        text);
  }

  private static TiInbound parseFailure(String reason, String text) {
    return new TiInbound.Unreadable(reason, text);
  }

  private TiInbound outward(Element header, Element root, String text) {
    String operation = childText(header, "Operation").orElse("");
    Optional<Element> payload = children(root).stream().filter(e -> e != header).findFirst();
    if (payload.isEmpty()) {
      return parseFailure("outward message without payload", text);
    }
    Map<String, String> f = new LinkedHashMap<>();
    List<TiInbound.Attachment> attachments = new ArrayList<>();
    for (Element field : children(payload.get())) {
      if ("AttachedDocuments".equals(field.getLocalName())) {
        attachment(field).ifPresent(attachments::add);
      } else if (!nil(field)) {
        f.putIfAbsent(field.getLocalName(), field.getTextContent().trim());
      }
    }
    String messageName = Optional.ofNullable(f.get("MessageName")).orElse(operation);
    return new TiInbound.Outward(
        operation,
        messageName,
        kind(messageName, operation),
        field(f, "eBankMasterRef"),
        field(f, "eBankEventRef"),
        field(f, "MasterRef"),
        field(f, "EventRef"),
        field(f, "BankComment"),
        field(f, "TFAmount"),
        field(f, "TFCurrency"),
        field(f, "OutstandingAmt"),
        field(f, "OutstandingCcy"),
        field(f, "MaturityDate").flatMap(TiMessageParser::date),
        field(f, "Status"),
        attachments,
        text);
  }

  /**
   * ASSUMPTION(Q39): the last three letters of MessageName or Operation say what the message
   * reports (TFFINACK carrying MessageName TFFSAACK in the bank's test).
   */
  static TiInbound.Kind kind(String messageName, String operation) {
    for (String name : List.of(messageName, operation)) {
      if (name.endsWith("ACK")) {
        return TiInbound.Kind.ACK;
      }
      if (name.endsWith("REJ")) {
        return TiInbound.Kind.REJ;
      }
      if (name.endsWith("CRT")) {
        return TiInbound.Kind.CRT;
      }
    }
    return TiInbound.Kind.OTHER;
  }

  private static Optional<TiInbound.Attachment> attachment(Element doc) {
    Map<String, String> f = new LinkedHashMap<>();
    for (Element e : children(doc)) {
      f.put(e.getLocalName(), e.getTextContent().trim());
    }
    String data = f.getOrDefault("DocDetails", "");
    if (data.isEmpty()) {
      return Optional.empty();
    }
    byte[] content;
    try {
      content = Base64.getMimeDecoder().decode(data);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
    return Optional.of(
        new TiInbound.Attachment(
            f.getOrDefault("DocumentId", ""),
            f.getOrDefault("DocumentDesc", ""),
            f.getOrDefault("Mime", "application/octet-stream"),
            content,
            f.getOrDefault("DocumentReference", "")));
  }

  private static Optional<String> field(Map<String, String> fields, String name) {
    return Optional.ofNullable(fields.get(name)).filter(v -> !v.isEmpty());
  }

  private static Optional<LocalDate> date(String value) {
    try {
      return Optional.of(LocalDate.parse(value));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  private static boolean nil(Element e) {
    return "true".equals(e.getAttributeNS(XSI, "nil"));
  }

  private static Document read(String text) throws Exception {
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    f.setNamespaceAware(true);
    f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    f.setXIncludeAware(false);
    f.setExpandEntityReferences(false);
    var builder = f.newDocumentBuilder();
    builder.setErrorHandler(null);
    return builder.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
  }

  private static List<Element> children(Element parent) {
    List<Element> out = new ArrayList<>();
    for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
      if (n instanceof Element e) {
        out.add(e);
      }
    }
    return out;
  }

  private static Optional<Element> child(Element parent, String localName) {
    return children(parent).stream().filter(e -> localName.equals(e.getLocalName())).findFirst();
  }

  private static Optional<String> childText(Element parent, String localName) {
    return child(parent, localName).map(e -> e.getTextContent().trim());
  }
}
