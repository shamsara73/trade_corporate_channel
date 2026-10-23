package com.bank.tradechannel.persistence;

import java.util.Locale;

/** SQL Server syntax. The only class allowed to contain vendor-specific SQL. */
public final class SqlServerDialect implements SqlDialect {

  @Override
  public String limit(String select, int rows) {
    if (rows < 1) {
      throw new IllegalArgumentException("rows must be positive: " + rows);
    }
    String trimmed = select.stripLeading();
    if (!trimmed.toUpperCase(Locale.ROOT).startsWith("SELECT ")) {
      throw new IllegalArgumentException("not a SELECT: " + select);
    }
    return "SELECT TOP " + rows + " " + trimmed.substring("SELECT ".length());
  }
}
