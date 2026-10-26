package com.bank.tradechannel.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FinanceDomainTest {
  static final Currency USD = Currency.getInstance("USD");
  static final Currency JPY = Currency.getInstance("JPY");
  final Clock clock = Clock.fixed(Instant.parse("2026-10-26T09:30:00Z"), ZoneOffset.UTC);

  static FinanceRequest request(LocalDate start, int tenor) {
    var amount = new Money(new BigDecimal("102"), USD);
    return new FinanceRequest(
        "REF-1", amount, amount, start, tenor, false, InterestFrom.AFTER, "PSF", "", "", "");
  }

  @Test
  void maturityIsStartPlusTenor_Q36() {
    assertThat(request(LocalDate.parse("2025-06-01"), 60).maturityDate())
        .isEqualTo(LocalDate.parse("2025-07-31")); // what TI booked in the bank's test
  }

  @Test
  void moneyKeepsTheCurrencyScale_Q35() {
    assertThat(new Money(new BigDecimal("102"), USD).plain()).isEqualTo("102.00");
    assertThat(new Money(new BigDecimal("5000"), JPY).plain()).isEqualTo("5000");
    assertThatThrownBy(() -> new Money(new BigDecimal("1.234"), USD))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Money(new BigDecimal("1.5"), JPY))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Money(BigDecimal.ZERO, USD))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void referencesHaveTheShapeOfTheBankSamples_Q34() {
    var refs = new ReferenceGenerator(clock, new Random(7)).newTransaction();
    assertThat(refs.eBankMasterRef())
        .matches("TF261026093000[0-9A-F]{2}")
        .hasSizeLessThanOrEqualTo(20);
    assertThat(refs.eBankEventRef())
        .matches("261026093000[0-9A-F]{2}")
        .hasSizeLessThanOrEqualTo(20);
    assertThat(refs.theirRef())
        .matches("MSG-20261026093000-[0-9A-F]{8}")
        .hasSizeLessThanOrEqualTo(34);
  }

  @Test
  void eachSendGetsNewReferencesAndGoesToTheGateway() {
    var sent = new java.util.ArrayList<FinanceSubmission>();
    TradeGateway gateway =
        s -> {
          sent.add(s);
          return new TradeGateway.SendReceipt("test");
        };
    var service =
        new FinanceRequestService(gateway, new ReferenceGenerator(clock, new Random(1)), clock);
    var customer = new Customer("0009990001", "POC TEST CUSTOMER", "001");
    var a = service.send(request(LocalDate.parse("2026-10-26"), 60), customer);
    var b = service.send(request(LocalDate.parse("2026-10-26"), 60), customer);
    assertThat(sent).hasSize(2);
    assertThat(a.submission().references().theirRef())
        .isNotEqualTo(b.submission().references().theirRef());
    assertThat(a.submission().customer()).isEqualTo(customer);
    assertThat(a.receipt().destination()).isEqualTo("test");
  }
}
