package com.bank.tradechannel.persistence;

import com.bank.tradechannel.domain.txn.UnitOfWork;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

/** The domain's unit of work on Spring's transaction manager. */
public final class JdbcUnitOfWork implements UnitOfWork {
  private final TransactionTemplate transactions;

  public JdbcUnitOfWork(TransactionTemplate transactions) {
    this.transactions = transactions;
  }

  @Override
  public <T> T inTransaction(Supplier<T> work) {
    return transactions.execute(status -> work.get());
  }
}
