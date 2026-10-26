package com.bank.tradechannel.domain.txn;

import java.util.function.Supplier;

/** Runs work in one database transaction: all of it commits, or none (ground rule 7). */
public interface UnitOfWork {
  <T> T inTransaction(Supplier<T> work);
}
