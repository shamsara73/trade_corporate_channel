package com.bank.tradechannel.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.tradechannel.domain.txn.Customer;
import com.bank.tradechannel.domain.txn.EventStatus;
import com.bank.tradechannel.domain.txn.EventType;
import com.bank.tradechannel.domain.txn.TradeEvent;
import com.bank.tradechannel.domain.txn.TradeTxn;
import com.bank.tradechannel.domain.txn.TxnStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The FSA rules of {@link FsaRules} and the event state machine. */
class FsaRulesTest {
  static final Instant AT = Instant.parse("2026-10-26T09:30:00Z");
  static final Currency USD = Currency.getInstance("USD");
  static final UUID TXN = UUID.randomUUID();

  static TradeTxn txn(TxnStatus status, String outstanding) {
    return new TradeTxn(
        TXN, new Customer("0009990001", "POC TEST CUSTOMER", "001"), "FSA", "TF26102609300011",
        "REF", status, "USD", new BigDecimal("102.00"),
        Optional.ofNullable(outstanding).map(BigDecimal::new), LocalDate.parse("2026-10-26"),
        LocalDate.parse("2026-12-25"), Optional.of("C001PSF0000001ID"), Optional.empty(), "demo",
        AT, AT, 0);
  }

  static TradeEvent event(EventType type, int seq, EventStatus status) {
    return new TradeEvent(
        UUID.randomUUID(), TXN, type, seq, "E" + seq, status, Optional.empty(), Optional.empty(),
        Optional.empty(), "demo", AT, AT, Optional.empty(), 0);
  }

  static RepaymentRequest repayment(RepaymentAction action, String principal, String valueDate) {
    return new RepaymentRequest(
        "RPY-1", action, LocalDate.parse(valueDate), RepaymentPayer.DEBIT_PARTY,
        new Money(new BigDecimal(principal), USD), BigDecimal.ZERO);
  }

  @Test
  void fsa1_noRepaymentBeforeTheCreateEventIsCompleted() {
    for (EventStatus s : List.of(EventStatus.PENDING, EventStatus.SENT, EventStatus.RECEIVED)) {
      assertThat(
              FsaRules.whyNoRepay(
                  txn(TxnStatus.PENDING, null), List.of(event(EventType.CREATE, 1, s))))
          .contains("fsa.repay.notYet");
    }
  }

  @Test
  void fsa1_repaymentAllowedOnceTheFinanceIsLive() {
    assertThat(
            FsaRules.whyNoRepay(
                txn(TxnStatus.LIVE, "102.00"),
                List.of(event(EventType.CREATE, 1, EventStatus.COMPLETED))))
        .isEmpty();
  }

  @Test
  void fsa1_noRepaymentOfARefusedOrRepaidFinance() {
    assertThat(
            FsaRules.whyNoRepay(
                txn(TxnStatus.REJECTED, null),
                List.of(event(EventType.CREATE, 1, EventStatus.REJECTED))))
        .contains("fsa.repay.notYet");
    assertThat(
            FsaRules.whyNoRepay(
                txn(TxnStatus.REPAID, "0.00"),
                List.of(
                    event(EventType.CREATE, 1, EventStatus.COMPLETED),
                    event(EventType.REPAY, 2, EventStatus.COMPLETED))))
        .contains("fsa.repay.closed");
    assertThat(
            FsaRules.whyNoRepay(
                txn(TxnStatus.LIVE, "0.00"),
                List.of(event(EventType.CREATE, 1, EventStatus.COMPLETED))))
        .contains("fsa.repay.nothingOutstanding");
  }

  @Test
  void fsa2_oneEventInFlightAtATime() {
    assertThat(
            FsaRules.whyNoRepay(
                txn(TxnStatus.LIVE, "102.00"),
                List.of(
                    event(EventType.CREATE, 1, EventStatus.COMPLETED),
                    event(EventType.REPAY, 2, EventStatus.SENT))))
        .contains("fsa.repay.inFlight");
    // A refused repayment leaves the finance open for another one.
    assertThat(
            FsaRules.whyNoRepay(
                txn(TxnStatus.LIVE, "102.00"),
                List.of(
                    event(EventType.CREATE, 1, EventStatus.COMPLETED),
                    event(EventType.REPAY, 2, EventStatus.REJECTED))))
        .isEmpty();
  }

  @Test
  void fsa3_principalWithinOutstandingAndFinalClearsIt() {
    var live = txn(TxnStatus.LIVE, "102.00");
    assertThat(FsaRules.checkRepayment(live, repayment(RepaymentAction.FINAL, "102.00", "2026-11-01")))
        .isEmpty();
    assertThat(
            FsaRules.checkRepayment(
                live, repayment(RepaymentAction.INTEREST_THEN_PRINCIPAL, "50.00", "2026-11-01")))
        .isEmpty();
    assertThat(FsaRules.checkRepayment(live, repayment(RepaymentAction.FINAL, "50.00", "2026-11-01")))
        .containsEntry("principal", "fsa.repay.finalMustClear");
    assertThat(
            FsaRules.checkRepayment(
                live, repayment(RepaymentAction.INTEREST_THEN_PRINCIPAL, "102.01", "2026-11-01")))
        .containsEntry("principal", "fsa.repay.tooMuch");
  }

  @Test
  void fsa3_repaymentInTheFinanceCurrency() {
    var eur =
        new RepaymentRequest(
            "RPY-1", RepaymentAction.MINIMUM, LocalDate.parse("2026-11-01"),
            RepaymentPayer.DEBIT_PARTY, new Money(new BigDecimal("10"), Currency.getInstance("EUR")),
            BigDecimal.ZERO);
    assertThat(FsaRules.checkRepayment(txn(TxnStatus.LIVE, "102.00"), eur))
        .containsEntry("principal", "fsa.repay.currency");
  }

  @Test
  void fsa4_valueDateNotBeforeTheStart() {
    assertThat(
            FsaRules.checkRepayment(
                txn(TxnStatus.LIVE, "102.00"),
                repayment(RepaymentAction.FINAL, "102.00", "2026-10-25")))
        .containsEntry("valueDate", "fsa.repay.beforeStart");
  }

  @Test
  void eventStateMachine() {
    assertThat(EventStatus.PENDING.canMoveTo(EventStatus.SENT)).isTrue();
    assertThat(EventStatus.PENDING.canMoveTo(EventStatus.RECEIVED)).isTrue(); // reply before mark
    assertThat(EventStatus.SENT.canMoveTo(EventStatus.RECEIVED)).isTrue();
    assertThat(EventStatus.RECEIVED.canMoveTo(EventStatus.COMPLETED)).isTrue();
    assertThat(EventStatus.RECEIVED.canMoveTo(EventStatus.REJECTED)).isTrue();
    // TI may change its mind with a later ACK or REJ.
    assertThat(EventStatus.COMPLETED.canMoveTo(EventStatus.REJECTED)).isTrue();
    assertThat(EventStatus.REJECTED.canMoveTo(EventStatus.COMPLETED)).isTrue();
    // Never back.
    assertThat(EventStatus.RECEIVED.canMoveTo(EventStatus.SENT)).isFalse();
    assertThat(EventStatus.COMPLETED.canMoveTo(EventStatus.RECEIVED)).isFalse();
    assertThat(EventStatus.FAILED.canMoveTo(EventStatus.COMPLETED)).isFalse();
    assertThat(EventStatus.COMPLETED.inFlight()).isFalse();
    assertThat(EventStatus.RECEIVED.inFlight()).isTrue();
  }

  @Test
  void movingAnEventRecordsTiDecision() {
    var e = event(EventType.CREATE, 1, EventStatus.RECEIVED);
    var later = AT.plusSeconds(60);
    var done =
        e.moveTo(EventStatus.COMPLETED, Optional.of("CRE001"), Optional.of("OK"), Optional.empty(), later);
    assertThat(done.status()).isEqualTo(EventStatus.COMPLETED);
    assertThat(done.tiEventRef()).contains("CRE001");
    assertThat(done.completedAt()).contains(later);
    assertThat(done.moveTo(EventStatus.SENT, Optional.empty(), Optional.empty(), Optional.empty(), later))
        .isSameAs(done);
  }
}
