package com.bank.tradechannel.gateway;

import com.bank.tradechannel.domain.finance.FinanceSubmission;
import com.bank.tradechannel.domain.finance.OutboundMessage;
import com.bank.tradechannel.domain.finance.RepaySubmission;
import com.bank.tradechannel.domain.finance.TradeGateway;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pretend TI for demos in file mode (never in production, ground rule 6): writes each message like
 * {@link FileTradeGateway}, then answers in the inbox directory the way the bank's TI did in its test:
 * a ServiceResponse at once and, after {@code completeAfter} (the back office "completing" the event),
 * an outward TFFINACK with the booked amounts in minor units and a debit/credit advice. Every message
 * is approved; drop a REJ file into the inbox by hand to try a refusal.
 */
public final class SimulatedTi implements TradeGateway, AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(SimulatedTi.class);
  private static final String CONTROL = "urn:control.services.tiplus2.misys.com";
  /** A one-page PDF saying "Simulated advice", so the download can be tried. */
  private static final String ADVICE_PDF =
      "JVBERi0xLjQKMSAwIG9iajw8L1R5cGUvQ2F0YWxvZy9QYWdlcyAyIDAgUj4+ZW5kb2JqCjIgMCBvYmo8PC9UeXBlL1Bh"
          + "Z2VzL0tpZHNbMyAwIFJdL0NvdW50IDE+PmVuZG9iagozIDAgb2JqPDwvVHlwZS9QYWdlL1BhcmVudCAyIDAgUi9NZW"
          + "RpYUJveFswIDAgMzAwIDEwMF0vQ29udGVudHMgNCAwIFIvUmVzb3VyY2VzPDwvRm9udDw8L0YxIDUgMCBSPj4+Pj4+"
          + "ZW5kb2JqCjQgMCBvYmo8PC9MZW5ndGggNDQ+PnN0cmVhbQpCVCAvRjEgMTQgVGYgMjAgNTAgVGQgKFNpbXVsYXRlZC"
          + "BhZHZpY2UpIFRqIEVUCmVuZHN0cmVhbQplbmRvYmoKNSAwIG9iajw8L1R5cGUvRm9udC9TdWJ0eXBlL1R5cGUxL0Jh"
          + "c2VGb250L0hlbHZldGljYT4+ZW5kb2JqCnRyYWlsZXI8PC9Sb290IDEgMCBSPj4KJSVFT0YK";

  private final FileTradeGateway files;
  private final Path inbox;
  private final Duration completeAfter;
  private final Clock clock;
  private final Map<String, Book> books = new ConcurrentHashMap<>();
  private final AtomicInteger masters = new AtomicInteger();
  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "simulated-ti");
            t.setDaemon(true);
            return t;
          });
  private final XMLOutputFactory xml = XMLOutputFactory.newFactory();

  public SimulatedTi(FileTradeGateway files, Path inbox, Duration completeAfter, Clock clock) {
    this.files = files;
    this.inbox = inbox;
    this.completeAfter = completeAfter;
    this.clock = clock;
  }

  @Override
  public SendReceipt send(OutboundMessage message) {
    SendReceipt receipt = files.send(message);
    String ref = message.references().theirRef();
    String operation = message instanceof FinanceSubmission ? "TFFINNEW" : "TFFSARPY";
    drop("1-reply-" + ref, reply(operation, ref));
    scheduler.schedule(
        () -> drop("2-ack-" + ref, ack(message)), completeAfter.toMillis(), TimeUnit.MILLISECONDS);
    return receipt;
  }

  @Override
  public void close() {
    scheduler.shutdownNow();
  }

  private String reply(String operation, String correlationId) {
    return write(
        x -> {
          x.writeStartElement(CONTROL, "ServiceResponse");
          x.writeDefaultNamespace(CONTROL);
          x.writeStartElement(CONTROL, "ResponseHeader");
          text(x, "Service", "TI");
          text(x, "Operation", operation);
          text(x, "Status", "SUCCEEDED");
          x.writeStartElement(CONTROL, "Details");
          text(x, "Warning", "Request for service: 'TI' operation: '" + operation
              + "' does not return any response data");
          x.writeEndElement();
          text(x, "CorrelationId", correlationId);
          text(x, "TargetSystem", "SIMULATOR");
          text(x, "SourceSystem", "ZONE1");
          x.writeEndElement();
          x.writeEndElement();
        });
  }

  private String ack(OutboundMessage message) {
    var refs = message.references();
    String eventRef;
    String currency;
    BigDecimal booked;
    LocalDate maturity;
    Book book;
    if (message instanceof FinanceSubmission create) {
      currency = create.request().financeAmount().currencyCode();
      booked = create.request().financeAmount().amount();
      maturity = create.request().maturityDate();
      book = new Book(String.format("SIM%06dFSA", masters.incrementAndGet()), currency, booked, maturity, 1);
      eventRef = "CRE001";
    } else {
      RepaySubmission repay = (RepaySubmission) message;
      // After a restart the pretend TI has forgotten its books: assume the repayment clears it.
      Book before =
          books.getOrDefault(
              refs.eBankMasterRef(),
              new Book(repay.tiMasterRef(), repay.request().principal().currencyCode(),
                  repay.request().principal().amount(), repay.request().valueDate(), 1));
      currency = repay.request().principal().currencyCode();
      BigDecimal left = before.outstanding().subtract(repay.request().principal().amount()).max(BigDecimal.ZERO);
      book = new Book(before.masterRef(), currency, left, before.maturity(), before.repayments() + 1);
      booked = repay.request().total();
      maturity = before.maturity();
      eventRef = String.format("RPY%03d", book.repayments() - 1);
    }
    books.put(refs.eBankMasterRef(), book);
    BigDecimal finalBooked = booked;
    LocalDate finalMaturity = maturity;
    return write(
        x -> {
          x.writeStartElement(CONTROL, "ServiceRequest");
          x.writeDefaultNamespace(CONTROL);
          x.writeStartElement(CONTROL, "RequestHeader");
          text(x, "Service", "GATEWAY");
          text(x, "Operation", "TFFINACK");
          text(x, "SourceSystem", "ZONE1");
          text(x, "CorrelationId", Integer.toString(Math.abs(refs.theirRef().hashCode() % 100000)));
          text(x, "CreationDate", LocalDate.now(clock).toString());
          x.writeEndElement();
          x.writeStartElement(CONTROL, "tffindet");
          text(x, "MessageName", "TFFSAACK");
          text(x, "eBankMasterRef", refs.eBankMasterRef());
          text(x, "eBankEventRef", refs.eBankEventRef());
          text(x, "MasterRef", book.masterRef());
          text(x, "EventRef", eventRef);
          text(x, "BankComment", "We accept this instruction (simulated)");
          text(x, "TFAmount", minor(finalBooked, currency));
          text(x, "TFCurrency", currency);
          text(x, "OutstandingAmt", minor(book.outstanding(), currency));
          text(x, "OutstandingCcy", currency);
          text(x, "MaturityDate", finalMaturity.toString());
          text(x, "Status", "LIV");
          x.writeStartElement(CONTROL, "AttachedDocuments");
          text(x, "DocumentId", "OCFCCREDR");
          text(x, "DocumentDesc", "Debit/Credit Advice (simulated)");
          text(x, "Mime", "application/pdf");
          text(x, "DocDetails", ADVICE_PDF);
          text(x, "DocumentReference", "SIM-" + refs.eBankEventRef());
          x.writeEndElement();
          x.writeEndElement();
          x.writeEndElement();
        });
  }

  /** TI's minor units: 102.00 USD is 10200. */
  private static String minor(BigDecimal amount, String currency) {
    int digits = Math.max(Currency.getInstance(currency).getDefaultFractionDigits(), 0);
    return amount.movePointRight(digits).setScale(0, java.math.RoundingMode.UNNECESSARY).toPlainString();
  }

  private void drop(String name, String content) {
    try {
      Files.createDirectories(inbox);
      Path tmp = inbox.resolve(name + ".tmp");
      Files.writeString(tmp, content, StandardCharsets.UTF_8);
      Files.move(tmp, inbox.resolve(clock.millis() + "-" + name + ".xml"));
    } catch (IOException e) {
      LOG.warn("simulated TI cannot write to {}: {}", inbox, e.getMessage());
    }
  }

  private String write(XmlBody body) {
    var out = new StringWriter();
    try {
      XMLStreamWriter x = xml.createXMLStreamWriter(out);
      x.writeStartDocument("UTF-8", "1.0");
      x.setDefaultNamespace(CONTROL);
      body.write(x);
      x.writeEndDocument();
      x.close();
    } catch (XMLStreamException e) {
      throw new IllegalStateException(e);
    }
    return out.toString();
  }

  private static void text(XMLStreamWriter x, String name, String value) throws XMLStreamException {
    x.writeStartElement(CONTROL, name);
    x.writeCharacters(value);
    x.writeEndElement();
  }

  @FunctionalInterface
  private interface XmlBody {
    void write(XMLStreamWriter x) throws XMLStreamException;
  }

  /** What the pretend TI has booked for a master. */
  private record Book(String masterRef, String currency, BigDecimal outstanding, LocalDate maturity, int repayments) {}
}
