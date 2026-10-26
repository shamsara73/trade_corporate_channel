package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.FinanceSubmission;
import com.bank.tradechannel.domain.finance.OutboundMessage;
import com.bank.tradechannel.domain.finance.RepaySubmission;
import com.bank.tradechannel.domain.txn.ChannelRefs;
import java.io.StringWriter;
import java.math.BigDecimal;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Writes the messages to TI in the ServiceRequest envelope of the bank's successful test
 * (docs/reference/working-samples.md, section 2). TFFINNEW follows that message element for element;
 * TFFSARPY follows TI's XSD jar (not yet proven against TI, ASSUMPTION(Q38)). The tests validate both
 * against the XSD jar.
 *
 * <p>The element names are written here until the generated catalogue exists (task T1), which
 * replaces them with path constants (ground rule 2).
 */
public final class TiMessageWriter {
  static final String CONTROL = "urn:control.services.tiplus2.misys.com";
  static final String MESSAGES = "urn:messages.service.ti.apps.tiplus2.misys.com";
  static final String COMMON = "urn:common.service.ti.apps.tiplus2.misys.com";
  static final String PRODUCT_CODE = "FSA";

  private final GatewayProperties properties;
  private final XMLOutputFactory factory = XMLOutputFactory.newFactory();

  public TiMessageWriter(GatewayProperties properties) {
    this.properties = properties;
  }

  public String write(OutboundMessage message) {
    var out = new StringWriter();
    try {
      XMLStreamWriter x = factory.createXMLStreamWriter(out);
      if (message instanceof FinanceSubmission create) {
        envelope(x, "TFFINNEW", create.references());
        financeCreate(x, create);
      } else {
        RepaySubmission repay = (RepaySubmission) message;
        envelope(x, "TFFSARPY", repay.references());
        financeRepay(x, repay);
      }
      x.writeEndElement();
      x.writeEndElement();
      x.writeEndDocument();
      x.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("cannot write the message to TI", e);
    }
    return out.toString();
  }

  private void envelope(XMLStreamWriter x, String operation, ChannelRefs refs)
      throws XMLStreamException {
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
    text(x, CONTROL, "Operation", operation);
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
    x.writeStartElement(MESSAGES, operation);
  }

  private static void financeCreate(XMLStreamWriter x, FinanceSubmission s)
      throws XMLStreamException {
    var r = s.request();
    var c = s.customer();
    var refs = s.references();
    context(x, c.mnemonic(), r.theirReference(), c.behalfOfBranch());
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
    amount(x, "Amount", r.transactionAmount().plain(), r.transactionAmount().currencyCode());
    x.writeStartElement(MESSAGES, "DebitParty");
    text(x, COMMON, "Customer", c.mnemonic());
    text(x, COMMON, "NameAddress", c.name());
    text(x, COMMON, "Reference", r.theirReference());
    x.writeEndElement();
    text(x, MESSAGES, "EffectiveDate", r.startDate().toString());
    amount(x, "FinanceAmount", r.financeAmount().plain(), r.financeAmount().currencyCode());
    text(x, MESSAGES, "InterestSettlementCcy", r.financeAmount().currencyCode());
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
  }

  /** TFFSARPY spells the channel references EBankMasterRef / EBankEventRef (capital E). */
  private static void financeRepay(XMLStreamWriter x, RepaySubmission s) throws XMLStreamException {
    var r = s.request();
    var c = s.customer();
    var refs = s.references();
    String ccy = r.principal().currencyCode();
    context(x, c.mnemonic(), r.repaymentReference(), c.behalfOfBranch());
    text(x, MESSAGES, "MasterRef", s.tiMasterRef());
    text(x, MESSAGES, "EBankMasterRef", refs.eBankMasterRef());
    text(x, MESSAGES, "EBankEventRef", refs.eBankEventRef());
    text(x, MESSAGES, "RepaymentAction", r.action().code());
    text(x, MESSAGES, "RepaymentReference", r.repaymentReference());
    text(x, MESSAGES, "ValueDate", r.valueDate().toString());
    text(x, MESSAGES, "RepaymentPayer", r.payer().code());
    amount(x, "PrincipalRepaymentAmount", r.principal().plain(), ccy);
    if (r.interest().signum() > 0) {
      amount(x, "InterestRepaymentAmount", r.interest().toPlainString(), ccy);
    }
    amount(x, "TotalRepaymentAmount", plain(r.total()), ccy);
  }

  private static void context(XMLStreamWriter x, String customer, String theirReference, String branch)
      throws XMLStreamException {
    x.writeStartElement(MESSAGES, "Context");
    text(x, COMMON, "Customer", customer);
    text(x, COMMON, "TheirReference", theirReference);
    text(x, COMMON, "BehalfOfBranch", branch);
    x.writeEndElement();
  }

  private static String plain(BigDecimal value) {
    return value.toPlainString();
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
