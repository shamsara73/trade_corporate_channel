package com.bank.tradechannel.domain.finance;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount in a currency. The scale may not exceed the currency's minor units (2 for USD, 0 for
 * JPY), because TI books and returns amounts in minor units (Q35).
 */
public record Money(BigDecimal amount, Currency currency) {

  public Money {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(currency, "currency");
    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("amount must be positive");
    }
    if (amount.stripTrailingZeros().scale() > currency.getDefaultFractionDigits()) {
      throw new IllegalArgumentException(
          "too many decimals for " + currency + ": " + amount.toPlainString());
    }
    amount = amount.setScale(Math.max(currency.getDefaultFractionDigits(), 0));
  }

  /** ISO 4217 code, for example USD. */
  public String currencyCode() {
    return currency.getCurrencyCode();
  }

  /** As the gateway writes it: plain decimal, scale of the currency (for example 102.00). */
  public String plain() {
    return amount.toPlainString();
  }
}
