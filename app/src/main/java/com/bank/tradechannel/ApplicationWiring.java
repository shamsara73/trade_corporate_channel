package com.bank.tradechannel;

import com.bank.tradechannel.domain.finance.FsaDetailStore;
import com.bank.tradechannel.domain.finance.FsaService;
import com.bank.tradechannel.domain.finance.OutboxDispatcher;
import com.bank.tradechannel.domain.finance.ReferenceGenerator;
import com.bank.tradechannel.domain.finance.TiInboundProcessor;
import com.bank.tradechannel.domain.finance.TradeGateway;
import com.bank.tradechannel.domain.txn.InboundStore;
import com.bank.tradechannel.domain.txn.OutboxStore;
import com.bank.tradechannel.domain.txn.TxnStore;
import com.bank.tradechannel.domain.txn.UnitOfWork;
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
  FsaService fsaService(
      TxnStore txns,
      FsaDetailStore details,
      OutboxStore outbox,
      UnitOfWork uow,
      ReferenceGenerator references,
      Clock clock) {
    return new FsaService(txns, details, outbox, uow, references, clock);
  }

  @Bean
  OutboxDispatcher outboxDispatcher(
      OutboxStore outbox,
      TxnStore txns,
      FsaDetailStore details,
      TradeGateway gateway,
      UnitOfWork uow,
      Clock clock) {
    return new OutboxDispatcher(outbox, txns, details, gateway, uow, clock);
  }

  @Bean
  TiInboundProcessor tiInboundProcessor(
      TxnStore txns,
      OutboxStore outbox,
      FsaDetailStore details,
      InboundStore inbound,
      UnitOfWork uow,
      Clock clock) {
    return new TiInboundProcessor(txns, outbox, details, inbound, uow, clock);
  }
}
