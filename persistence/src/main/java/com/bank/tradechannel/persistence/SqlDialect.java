package com.bank.tradechannel.persistence;

/**
 * The few statements that differ between databases. SQL Server is the target; keeping these behind
 * one interface keeps Oracle possible (ADR 0003).
 */
public interface SqlDialect {

  /** Restricts a SELECT to its first {@code rows} rows. */
  String limit(String select, int rows);
}
