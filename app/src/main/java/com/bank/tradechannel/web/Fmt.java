package com.bank.tradechannel.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Formatting for templates ({@code ${@fmt.time(...)}}); times are shown in UTC. */
@Component("fmt")
public class Fmt {
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

  public String time(Instant instant) {
    return TIME.format(instant);
  }

  public String time(Optional<Instant> instant) {
    return instant.map(this::time).orElse("");
  }

  public String amount(String currency, BigDecimal amount) {
    return currency + " " + amount.toPlainString();
  }

  public String amount(String currency, Optional<BigDecimal> amount) {
    return amount.map(a -> amount(currency, a)).orElse("");
  }

  public String text(Optional<String> value) {
    return value.orElse("");
  }
}
