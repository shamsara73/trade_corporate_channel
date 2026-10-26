package com.bank.tradechannel;

import com.bank.tradechannel.domain.finance.FinanceRequestService;
import com.bank.tradechannel.domain.finance.ReferenceGenerator;
import com.bank.tradechannel.domain.finance.TradeGateway;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the domain services, which stay free of Spring annotations. */
@Configuration
class ApplicationWiring {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  ReferenceGenerator referenceGenerator(Clock clock) {
    return new ReferenceGenerator(clock, new SecureRandom());
  }

  @Bean
  FinanceRequestService financeRequestService(
      TradeGateway gateway, ReferenceGenerator references, Clock clock) {
    return new FinanceRequestService(gateway, references, clock);
  }
}
