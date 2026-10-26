package com.bank.tradechannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.tradechannel.domain.finance.FinanceRequest;
import com.bank.tradechannel.domain.finance.FsaDetailStore;
import com.bank.tradechannel.domain.finance.ReferenceGenerator;
import com.bank.tradechannel.domain.finance.FsaService;
import com.bank.tradechannel.domain.finance.InterestFrom;
import com.bank.tradechannel.domain.finance.Money;
import com.bank.tradechannel.domain.finance.OutboxDispatcher;
import com.bank.tradechannel.domain.finance.RepaymentAction;
import com.bank.tradechannel.domain.finance.RepaymentPayer;
import com.bank.tradechannel.domain.finance.RepaymentRefusedException;
import com.bank.tradechannel.domain.finance.RepaymentRequest;
import com.bank.tradechannel.domain.finance.TradeGateway;
import com.bank.tradechannel.domain.txn.Customer;
import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.InboundStore.Outcome;
import com.bank.tradechannel.domain.txn.OutboxEntry;
import com.bank.tradechannel.domain.txn.OutboxEntry.OutboxStatus;
import com.bank.tradechannel.domain.txn.OutboxStore;
import com.bank.tradechannel.domain.txn.TxnStatus;
import com.bank.tradechannel.domain.txn.TxnStore;
import com.bank.tradechannel.domain.txn.UnitOfWork;
import com.bank.tradechannel.gateway.InboundRouter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A Finance Standalone from request to repaid, through the real database (H2 in SQL Server mode),
 * outbox, file gateway and inbound processing. TI's answers are the samples in docs/samples with
 * this transaction's references put in.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FsaFlowTest {
  static final Customer CUSTOMER = new Customer("0009990001", "POC TEST CUSTOMER", "001");
  static final Currency USD = Currency.getInstance("USD");

  @Autowired MockMvc mvc;
  @Autowired FsaService service;
  @Autowired OutboxDispatcher dispatcher;
  @Autowired OutboxStore outbox;
  @Autowired InboundRouter router;
  @Autowired JdbcClient jdbc;
  @Autowired TxnStore txns;
  @Autowired FsaDetailStore details;
  @Autowired UnitOfWork uow;

  static final String CREATE = "finance-standalone/create/";
  static final String REPAY = "finance-standalone/repay/";

  static String fixture(String path) {
    return Samples.read(path);
  }

  static FinanceRequest request() {
    var amount = new Money(new BigDecimal("102.00"), USD);
    return new FinanceRequest(
        "FLOW-1", amount, amount, LocalDate.parse("2026-10-26"), 60, false, InterestFrom.AFTER,
        "PSF", "Goods", "", "");
  }

  static RepaymentRequest repayment(RepaymentAction action, String principal) {
    return new RepaymentRequest(
        "RPY-1", action, LocalDate.parse("2026-11-20"), RepaymentPayer.DEBIT_PARTY,
        new Money(new BigDecimal(principal), USD), BigDecimal.ZERO);
  }

  /** The latest delivery of an event of the transaction. */
  OutboxEntry delivery(UUID txn, EventType type) {
    return service.view(CUSTOMER, txn).orElseThrow().events().stream()
        .filter(e -> e.event().type() == type)
        .reduce((a, b) -> b)
        .orElseThrow()
        .delivery()
        .orElseThrow();
  }

  String serviceResponse(String theirRef, String status) {
    return fixture(CREATE + "2-ti-reply-ServiceResponse.xml")
        .replace("MSG-20261023093000-0A1B2C3D", theirRef)
        .replace("<Status>SUCCEEDED</Status>", "<Status>" + status + "</Status>");
  }

  /**
   * TI's completion of the event: the bank's TFFINACK for CREATE, the proposed TFFRPACK for REPAY
   * (docs/samples), with this transaction's references and TI's outstanding amount in minor units.
   */
  String ack(UUID txn, EventType type, String outstandingMinor) {
    boolean create = type == EventType.CREATE;
    String sample =
        create ? CREATE + "3-ti-response-TFFINACK.xml" : REPAY + "3-ti-response-TFFRPACK.xml";
    return withRefs(fixture(sample), txn, type)
        .replace(create ? "<OutstandingAmt>10200</OutstandingAmt>" : "<OutstandingAmt>5200</OutstandingAmt>",
            "<OutstandingAmt>" + outstandingMinor + "</OutstandingAmt>")
        .replace("<MaturityDate>2025-07-31</MaturityDate>", "<MaturityDate>2026-12-25</MaturityDate>");
  }

  /** TI's refusal: TFFINREJ (mapping from the bank) for CREATE, the proposed TFFRPREJ for REPAY. */
  String rej(UUID txn, EventType type) {
    return withRefs(
        fixture(type == EventType.CREATE
            ? CREATE + "3-ti-response-TFFINREJ.xml"
            : REPAY + "3-ti-response-TFFRPREJ.xml"),
        txn, type);
  }

  private String withRefs(String sample, UUID txn, EventType type) {
    var view = service.view(CUSTOMER, txn).orElseThrow();
    var event =
        view.events().stream().filter(e -> e.event().type() == type).reduce((a, b) -> b).orElseThrow();
    return sample
        .replace("TF2610230930001A", view.txn().eBankMasterRef())
        .replace(type == EventType.CREATE ? "26102309300077" : "26102409300088",
            event.event().eBankEventRef());
  }

  /** Created through the page, so the customer comes from the configuration. */
  UUID createThroughThePage() throws Exception {
    var post =
        post("/finance").with(user("demo")).with(csrf())
            .param("theirReference", "FLOW-1").param("currency", "USD")
            .param("financeAmount", "102.00").param("transactionAmount", "")
            .param("startDate", "2026-10-26").param("tenorDays", "60")
            .param("productType", "PSF").param("interestFrom", "AFTER");
    String location = mvc.perform(post).andReturn().getResponse().getRedirectedUrl();
    return UUID.fromString(location.substring("/transactions/".length()));
  }

  @Test
  void createThenRepayInFull() throws Exception {
    UUID id = createThroughThePage();

    // The outbox dispatcher delivers TFFINNEW.
    dispatcher.dispatchDue();
    OutboxEntry sent = delivery(id, EventType.CREATE);
    assertThat(sent.status()).isEqualTo(OutboxStatus.SENT);
    assertThat(Files.readString(Path.of(sent.destination().orElseThrow())))
        .contains("<Operation>TFFINNEW</Operation>")
        .contains("<CorrelationId>" + sent.theirRef() + "</CorrelationId>");
    assertThat(event(id, EventType.CREATE)).isEqualTo(EventStatus.SENT);

    // A repayment cannot start before TI completed the CREATE event (FSA-1).
    mvc.perform(get("/transactions/{id}/repay", id).with(user("demo")))
        .andExpect(redirectedUrl("/transactions/" + id))
        .andExpect(flash().attribute("notice", "fsa.repay.notYet"));
    assertThatThrownBy(() -> service.repay(CUSTOMER, id, repayment(RepaymentAction.FINAL, "102.00"), "demo"))
        .isInstanceOf(RepaymentRefusedException.class);

    // TI's immediate ServiceResponse, then the back office's TFFINACK.
    assertThat(router.accept(serviceResponse(sent.theirRef(), "SUCCEEDED"), "test")).isEqualTo(Outcome.MATCHED);
    assertThat(event(id, EventType.CREATE)).isEqualTo(EventStatus.RECEIVED);
    mvc.perform(get("/transactions/{id}", id).with(user("demo")))
        .andExpect(content().string(containsString("http-equiv=\"refresh\"")));

    String ack = ack(id, EventType.CREATE, "10200");
    assertThat(router.accept(ack, "test")).isEqualTo(Outcome.MATCHED);
    assertThat(router.accept(ack, "test")).isEqualTo(Outcome.DUPLICATE);

    var view = service.view(CUSTOMER, id).orElseThrow();
    assertThat(view.txn().status()).isEqualTo(TxnStatus.LIVE);
    assertThat(view.txn().tiMasterRef()).contains("C001PSF0000001ID");
    assertThat(view.txn().outstanding()).hasValueSatisfying(o -> assertThat(o).isEqualByComparingTo("102.00"));
    assertThat(view.events().get(0).event().tiEventRef()).contains("CRE001");
    assertThat(view.events().get(0).event().bankComment()).contains("We accept this financing request");
    assertThat(view.canRepay()).isTrue();
    assertThat(view.documents()).singleElement().satisfies(d -> assertThat(d.mimeType()).isEqualTo("application/pdf"));
    UUID document = view.documents().get(0).id();

    mvc.perform(get("/transactions/{id}", id).with(user("demo")))
        .andExpect(content().string(containsString("Repay this finance")))
        .andExpect(content().string(containsString("C001PSF0000001ID")))
        .andExpect(content().string(containsString("Debit/Credit Advice")))
        .andExpect(content().string(not(containsString("http-equiv=\"refresh\""))));
    mvc.perform(get("/transactions/{id}/documents/{doc}", id, document).with(user("demo")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"))
        .andExpect(header().string("Content-Disposition", containsString("OCFCCREDR.pdf")));

    // The repayment form: a final repayment must clear the outstanding (FSA-3).
    mvc.perform(get("/transactions/{id}/repay", id).with(user("demo")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"102.00\"")));
    mvc.perform(repayPost(id, "FINAL", "50.00"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("A final repayment must repay everything outstanding.")));
    mvc.perform(repayPost(id, "FINAL", "102.00")).andExpect(redirectedUrl("/transactions/" + id));

    // Only one event with TI at a time (FSA-2).
    mvc.perform(repayPost(id, "FINAL", "102.00"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Another event of this transaction is with the bank.")));

    dispatcher.dispatchDue();
    OutboxEntry repaySent = delivery(id, EventType.REPAY);
    assertThat(repaySent.operation()).isEqualTo("TFFSARPY");
    assertThat(Files.readString(Path.of(repaySent.destination().orElseThrow())))
        .contains("<m:MasterRef>C001PSF0000001ID</m:MasterRef>")
        .contains("<m:RepaymentAction>F</m:RepaymentAction>");
    router.accept(serviceResponse(repaySent.theirRef(), "SUCCEEDED"), "test");
    assertThat(router.accept(ack(id, EventType.REPAY, "0"), "test")).isEqualTo(Outcome.MATCHED);

    view = service.view(CUSTOMER, id).orElseThrow();
    assertThat(view.txn().status()).isEqualTo(TxnStatus.REPAID);
    assertThat(view.txn().outstanding()).hasValueSatisfying(o -> assertThat(o).isEqualByComparingTo("0"));
    assertThat(view.events()).extracting(e -> e.event().status())
        .containsExactly(EventStatus.COMPLETED, EventStatus.COMPLETED);
    assertThat(view.whyNoRepay()).contains("fsa.repay.closed");
  }

  @Test
  void partialRepaymentWithoutTiFigureLowersTheOutstanding() throws Exception {
    UUID id = live();
    service.repay(CUSTOMER, id, repayment(RepaymentAction.INTEREST_THEN_PRINCIPAL, "40.00"), "demo");
    dispatcher.dispatchDue();
    // TI's ACK without an outstanding amount: the channel subtracts the principal itself.
    String ack = ack(id, EventType.REPAY, "0").replace("<OutstandingAmt>0</OutstandingAmt>", "<OutstandingAmt xsi:nil=\"true\"/>");
    router.accept(ack, "test");
    var txn = service.view(CUSTOMER, id).orElseThrow().txn();
    assertThat(txn.status()).isEqualTo(TxnStatus.LIVE);
    assertThat(txn.outstanding()).hasValueSatisfying(o -> assertThat(o).isEqualByComparingTo("62.00"));
  }

  @Test
  void aRefusedRepaymentLeavesTheFinanceOpen() throws Exception {
    UUID id = live();
    service.repay(CUSTOMER, id, repayment(RepaymentAction.FINAL, "102.00"), "demo");
    dispatcher.dispatchDue();
    assertThat(router.accept(rej(id, EventType.REPAY), "test")).isEqualTo(Outcome.MATCHED);
    var view = service.view(CUSTOMER, id).orElseThrow();
    assertThat(view.events().get(1).event().status()).isEqualTo(EventStatus.REJECTED);
    assertThat(view.txn().status()).isEqualTo(TxnStatus.LIVE);
    assertThat(view.canRepay()).isTrue();
  }

  @Test
  void aRefusedCreateClosesTheTransaction() throws Exception {
    UUID id = service.create(CUSTOMER, request(), "demo").id();
    dispatcher.dispatchDue();
    router.accept(rej(id, EventType.CREATE), "test");
    var view = service.view(CUSTOMER, id).orElseThrow();
    assertThat(view.txn().status()).isEqualTo(TxnStatus.REJECTED);
    assertThat(view.whyNoRepay()).contains("fsa.repay.notYet");
  }

  @Test
  void aFailedServiceResponseFailsTheEvent() throws Exception {
    UUID id = service.create(CUSTOMER, request(), "demo").id();
    dispatcher.dispatchDue();
    String failed =
        serviceResponse(delivery(id, EventType.CREATE).theirRef(), "FAILED")
            .replace("<Warning>", "<Error>").replace("</Warning>", "</Error>");
    router.accept(failed, "test");
    var view = service.view(CUSTOMER, id).orElseThrow();
    assertThat(view.events().get(0).event().status()).isEqualTo(EventStatus.FAILED);
    assertThat(view.events().get(0).event().errorText()).hasValueSatisfying(t -> assertThat(t).contains("Error"));
    assertThat(view.txn().status()).isEqualTo(TxnStatus.FAILED);
  }

  @Test
  void unmatchedAndUnreadableMessagesAreKept() throws Exception {
    long before = jdbc.sql("SELECT COUNT(*) FROM inbound_msg").query(Long.class).single();
    String stranger =
        fixture(CREATE + "3-ti-response-TFFINACK.xml").replace("TF2610230930001A", "TF-NOT-OURS").replace("26102309300077", "NOPE");
    assertThat(router.accept(stranger, "test")).isEqualTo(Outcome.UNMATCHED);
    assertThat(router.accept("garbage", "test")).isEqualTo(Outcome.UNREADABLE);
    assertThat(jdbc.sql("SELECT COUNT(*) FROM inbound_msg").query(Long.class).single()).isEqualTo(before + 2);
  }

  @Test
  void anotherCustomersTransactionIsNotFound_groundRule8() throws Exception {
    var other = new Customer("0009990002", "OTHER TEST CUSTOMER", "001");
    UUID id = service.create(other, request(), "someone").id();
    mvc.perform(get("/transactions/{id}", id).with(user("demo"))).andExpect(status().isNotFound());
    mvc.perform(get("/transactions/{id}/repay", id).with(user("demo"))).andExpect(status().isNotFound());
    mvc.perform(get("/transactions").with(user("demo")))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString(id.toString()))));
  }

  @Test
  void aClaimedMessageIsNotClaimedAgainUntilItsLeaseExpires() {
    UUID id = service.create(CUSTOMER, request(), "demo").id();
    Instant now = Instant.now();
    var first = outbox.claimDue(now.plusSeconds(1), now.minusSeconds(120), 100);
    assertThat(first).anySatisfy(e -> assertThat(delivery(id, EventType.CREATE).id()).isEqualTo(e.id()));
    assertThat(outbox.claimDue(now.plusSeconds(1), now.minusSeconds(120), 100))
        .noneSatisfy(e -> assertThat(e.id()).isEqualTo(delivery(id, EventType.CREATE).id()));
    // A crashed dispatcher's lease runs out: the message is claimed again.
    assertThat(outbox.claimDue(now.plusSeconds(600), now.plusSeconds(300), 100))
        .anySatisfy(e -> assertThat(e.id()).isEqualTo(delivery(id, EventType.CREATE).id()));
    dispatcher.dispatchDue();
  }

  @Test
  void aTakenReferenceIsDrawnAgain_Q34() {
    // Calls 0-2 (first request) and 3-5 (second request, first try) draw the same values.
    var calls = new AtomicLong();
    RandomGenerator scripted =
        () -> {
          long n = calls.getAndIncrement();
          return (n < 6 ? n % 3 : n) << 32;
        };
    Clock frozen = Clock.fixed(Instant.parse("2026-10-26T09:30:00Z"), ZoneOffset.UTC);
    var generator = new ReferenceGenerator(frozen, scripted);
    var sameSecond = new FsaService(txns, details, outbox, uow, generator, frozen);
    var first = sameSecond.create(CUSTOMER, request(), "demo");
    var second = sameSecond.create(CUSTOMER, request(), "demo");
    assertThat(second.eBankMasterRef()).isNotEqualTo(first.eBankMasterRef());
    assertThat(calls.get()).isEqualTo(9);
    dispatcher.dispatchDue();
  }

  @Test
  void aDeliveredMessageIsRecordedAsSentEvenWithALongDestination() {
    UUID id = service.create(CUSTOMER, request(), "demo").id();
    TradeGateway longPath = m -> new TradeGateway.SendReceipt("/" + "d".repeat(700), "<x/>");
    new OutboxDispatcher(outbox, txns, details, longPath, uow, Clock.systemUTC()).dispatchDue();
    OutboxEntry sent = delivery(id, EventType.CREATE);
    assertThat(sent.status()).isEqualTo(OutboxStatus.SENT);
    assertThat(sent.attempts()).isEqualTo(1);
    assertThat(sent.destination()).hasValueSatisfying(d -> assertThat(d).hasSize(512));
  }

  /** A finance TI has booked. */
  UUID live() throws Exception {
    UUID id = service.create(CUSTOMER, request(), "demo").id();
    dispatcher.dispatchDue();
    router.accept(serviceResponse(delivery(id, EventType.CREATE).theirRef(), "SUCCEEDED"), "test");
    router.accept(ack(id, EventType.CREATE, "10200"), "test");
    assertThat(service.view(CUSTOMER, id).orElseThrow().canRepay()).isTrue();
    return id;
  }

  EventStatus event(UUID txn, EventType type) {
    return service.view(CUSTOMER, txn).orElseThrow().events().stream()
        .filter(e -> e.event().type() == type)
        .reduce((a, b) -> b)
        .orElseThrow()
        .event()
        .status();
  }

  static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder repayPost(
      UUID id, String action, String principal) {
    return post("/transactions/{id}/repay", id)
        .with(user("demo"))
        .with(csrf())
        .param("repaymentReference", "RPY-1")
        .param("action", action)
        .param("valueDate", "2026-11-20")
        .param("payer", "DEBIT_PARTY")
        .param("principal", principal)
        .param("interest", "");
  }
}
