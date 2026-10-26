package com.bank.tradechannel.persistence;

import com.bank.tradechannel.domain.txn.DuplicateReferenceException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Conversions between Java values and columns: UUIDs as CHAR(36), times as UTC DATETIME2. */
final class Db {
  private Db() {}

  static String id(UUID id) {
    return id.toString();
  }

  static UUID uuid(ResultSet rs, String column) throws SQLException {
    return UUID.fromString(rs.getString(column).trim());
  }

  static Optional<UUID> optionalUuid(ResultSet rs, String column) throws SQLException {
    return Optional.ofNullable(rs.getString(column)).map(String::trim).map(UUID::fromString);
  }

  static LocalDateTime utc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  static LocalDateTime utc(Optional<Instant> instant) {
    return instant.map(Db::utc).orElse(null);
  }

  static Instant instant(ResultSet rs, String column) throws SQLException {
    return optionalInstant(rs, column).orElseThrow();
  }

  static Optional<Instant> optionalInstant(ResultSet rs, String column) throws SQLException {
    Timestamp ts = rs.getTimestamp(column);
    return Optional.ofNullable(ts).map(t -> t.toLocalDateTime().toInstant(ZoneOffset.UTC));
  }

  static Optional<String> text(ResultSet rs, String column) throws SQLException {
    return Optional.ofNullable(rs.getString(column));
  }

  /** SQL Server keeps '' and NULL apart, Oracle does not (ADR 0003): store neither for "no value". */
  static String orNull(Optional<String> value) {
    return value.filter(v -> !v.isBlank()).orElse(null);
  }

  static String orNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  static String orEmpty(ResultSet rs, String column) throws SQLException {
    return Optional.ofNullable(rs.getString(column)).orElse("");
  }

  /**
   * An amount column (stored with 3 decimals) at the scale of its currency, so 102.000 USD reads as
   * 102.00. A value with more decimals than the currency has is kept as stored.
   */
  static Optional<BigDecimal> amount(ResultSet rs, String column, String currency)
      throws SQLException {
    BigDecimal value = rs.getBigDecimal(column);
    if (value == null) {
      return Optional.empty();
    }
    int digits = Math.max(Currency.getInstance(currency.trim()).getDefaultFractionDigits(), 0);
    BigDecimal stripped = value.stripTrailingZeros();
    return Optional.of(stripped.scale() > digits ? value : stripped.setScale(digits));
  }

  /** Runs an INSERT whose only unique key besides the id is a generated reference. */
  static void insertUnique(JdbcClient.StatementSpec insert, String reference) {
    try {
      insert.update();
    } catch (DuplicateKeyException e) {
      throw new DuplicateReferenceException(reference, e);
    }
  }

  /** Free text from TI or from an exception, cut to its column so storing it cannot fail. */
  static String clip(Optional<String> value, int max) {
    return value.map(v -> v.length() <= max ? v : v.substring(0, max)).orElse(null);
  }
}
