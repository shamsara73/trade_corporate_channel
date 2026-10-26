package com.bank.tradechannel.jobs;

import com.bank.tradechannel.domain.finance.OutboxDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Background work in the application (ADR 0002). Switched off with {@code channel.jobs.enabled=false}
 * (the tests drive the work themselves).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "channel.jobs.enabled", havingValue = "true", matchIfMissing = true)
class JobsConfig {

  @Bean
  OutboxJob outboxJob(OutboxDispatcher dispatcher) {
    return new OutboxJob(dispatcher);
  }
}
