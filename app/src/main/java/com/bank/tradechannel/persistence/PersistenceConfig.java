package com.bank.tradechannel.persistence;

import com.bank.tradechannel.domain.finance.FsaDetailStore;
import com.bank.tradechannel.domain.txn.InboundStore;
import com.bank.tradechannel.domain.txn.OutboxStore;
import com.bank.tradechannel.domain.txn.TxnStore;
import com.bank.tradechannel.domain.txn.UnitOfWork;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** The domain's stores on JDBC; the schema is applied by Liquibase (db/changelog). */
@Configuration
class PersistenceConfig {

  @Bean
  TxnStore txnStore(JdbcClient jdbc) {
    return new JdbcTxnStore(jdbc);
  }

  @Bean
  FsaDetailStore fsaDetailStore(JdbcClient jdbc) {
    return new JdbcFsaDetailStore(jdbc);
  }

  @Bean
  OutboxStore outboxStore(JdbcClient jdbc) {
    return new JdbcOutboxStore(jdbc);
  }

  @Bean
  InboundStore inboundStore(JdbcClient jdbc) {
    return new JdbcInboundStore(jdbc);
  }

  @Bean
  UnitOfWork unitOfWork(TransactionTemplate transactions) {
    return new JdbcUnitOfWork(transactions);
  }
}
