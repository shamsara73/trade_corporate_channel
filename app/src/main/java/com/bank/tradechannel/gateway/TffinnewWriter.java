package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.FinanceSubmission;
import java.io.StringWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Writes TFFINNEW in the ServiceRequest envelope, element for element as in the message the bank's
 * TI accepted (docs/reference/working-samples.md, section 2), validated against TI's XSD jar by the
 * tests.
 *
 * <p>The element names are written here until the generated catalogue exists (task T1), which
 * replaces them with path constants (ground rule 2).
 */
public final class TffinnewWriter {
  static final String CONTROL = "urn:control.services.tiplus2.misys.com";
  static final String MESSAGES = "urn:messages.service.ti.apps.tiplus2.misys.com";
  static final String COMMON = "urn:common.service.ti.apps.tiplus2.misys.com";
  static final String OPERATION = "TFFINNEW";
  static final String PRODUCT_CODE = "FSA";

  private final GatewayProperties properties;
  private final XMLOutputFactory factory = XMLOutputFactory.newFactory();

  public TffinnewWriter(GatewayProperties properties) {
    this.properties = properties;
  }

  public String write(FinanceSubmission s) {
    var r = s.request();
    var c = s.customer();
    var refs = s.references();
    var out = new StringWriter();
    try {
      XMLStreamWriter x = factory.createXMLStreamWriter(out);
      x.writeStartDocument("UTF-8", "1.0");
      x.setDefaultNamespace(CONTROL);
      x.setPrefix("m", MESSAGES);
      x.setPrefix("c", COMMON);
      x.writeStartElement(CONTROL, "ServiceRequest");
      x.writeDefaultNamespace(CONTROL);
      x.writeNamespace("m", MESSAGES);
      x.writeNamespace("c", COMMON);

      x.writeStartElement(CONTROL, "RequestHeader");
      text(x, CONTROL, "Service", "TI");
      text(x, CONTROL, "Operation", OPERATION);
      x.writeStartElement(CONTROL, "Credentials");
      text(x, CONTROL, "Name", properties.tiUser());
      x.writeEndElement();
      text(x, CONTROL, "ReplyFormat", "FULL");
      text(x, CONTROL, "TargetSystem", properties.targetSystem());
      text(x, CONTROL, "SourceSystem", properties.sourceSystem());
      text(x, CONTROL, "NoRepair", "Y");
      text(x, CONTROL, "NoOverride", "Y");
      text(x, CONTROL, "CorrelationId", refs.theirRef());
      text(x, CONTROL, "TransactionControl", "NONE");
      x.writeEndElement();

      x.writeStartElement(MESSAGES, OPERATION);
      x.writeStartElement(MESSAGES, "Context");
      text(x, COMMON, "Customer", c.mnemonic());
      text(x, COMMON, "TheirReference", r.theirReference());
      text(x, COMMON, "BehalfOfBranch", c.behalfOfBranch());
      x.writeEndElement();

      if (!r.goodsDescription().isBlank()
          || !r.messageToBank().isBlank()
          || !r.additionalInformation().isBlank()) {
        x.writeStartElement(MESSAGES, "EventNotificationss");
        x.writeStartElement(MESSAGES, "EventNotifications");
        text(x, MESSAGES, "MessageData", r.messageToBank());
        text(x, MESSAGES, "MessageDescription", r.goodsDescription());
        text(x, MESSAGES, "MessageInfo", r.additionalInformation());
        text(x, MESSAGES, "Actioned", "N");
        x.writeEndElement();
        x.writeEndElement();
      }

      amount(
          x,
          "Amount",
          r.transactionAmount().plain(),
          r.transactionAmount().currency().getCurrencyCode());
      x.writeStartElement(MESSAGES, "DebitParty");
      text(x, COMMON, "Customer", c.mnemonic());
      text(x, COMMON, "NameAddress", c.name());
      text(x, COMMON, "Reference", r.theirReference());
      x.writeEndElement();
      text(x, MESSAGES, "EffectiveDate", r.startDate().toString());
      amount(
          x,
          "FinanceAmount",
          r.financeAmount().plain(),
          r.financeAmount().currency().getCurrencyCode());
      text(x, MESSAGES, "InterestSettlementCcy", r.financeAmount().currency().getCurrencyCode());
      text(x, MESSAGES, "MaturityDate", r.maturityDate().toString());
      text(x, MESSAGES, "StartDate", r.startDate().toString());
      text(x, MESSAGES, "TrustReceiptLoan", r.trustReceiptLoan() ? "Y" : "N");
      text(x, MESSAGES, "InterestFromAfter", r.interestFrom().code());
      text(x, MESSAGES, "eBankMasterRef", refs.eBankMasterRef());
      text(x, MESSAGES, "eBankEventRef", refs.eBankEventRef());
      text(x, MESSAGES, "ProductCode", PRODUCT_CODE);
      x.writeStartElement(MESSAGES, "FinancePeriod");
      text(x, COMMON, "TenorDays", Integer.toString(r.tenorDays()));
      text(x, COMMON, "TenorPeriod", "D");
      x.writeEndElement();
      text(x, MESSAGES, "TheirRef", refs.theirRef());
      text(x, MESSAGES, "ProductType", r.productType());

      x.writeEndElement();
      x.writeEndElement();
      x.writeEndDocument();
      x.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("cannot write " + OPERATION, e);
    }
    return out.toString();
  }

  private static void amount(XMLStreamWriter x, String element, String amount, String currency)
      throws XMLStreamException {
    x.writeStartElement(MESSAGES, element);
    text(x, COMMON, "Amount", amount);
    text(x, COMMON, "Currency", currency);
    x.writeEndElement();
  }

  private static void text(XMLStreamWriter x, String ns, String name, String value)
      throws XMLStreamException {
    x.writeStartElement(ns, name);
    x.writeCharacters(value);
    x.writeEndElement();
  }
}
