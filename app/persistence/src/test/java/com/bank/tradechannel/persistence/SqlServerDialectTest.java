package com.bank.tradechannel.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SqlServerDialectTest {
  private final SqlDialect dialect = new SqlServerDialect();

  @Test
  void limitUsesTop() {
    assertThat(dialect.limit("SELECT id FROM outbox ORDER BY seq", 10))
        .isEqualTo("SELECT TOP 10 id FROM outbox ORDER BY seq");
  }

  @Test
  void limitRejectsNonSelectAndBadRowCount() {
    assertThatThrownBy(() -> dialect.limit("DELETE FROM outbox", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> dialect.limit("SELECT 1", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
