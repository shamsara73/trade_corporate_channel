package com.bank.tradechannel.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Applies the master changelog to a fresh SQL Server. Runs with {@code ./mvnw verify
 * -Pintegration}.
 */
@Testcontainers
class LiquibaseMigrationIT {

  @Container
  static final MSSQLServerContainer<?> SQL_SERVER =
      new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04")
          .acceptLicense();

  @Test
  void masterChangelogAppliesAndValidates() throws Exception {
    try (Connection connection =
        DriverManager.getConnection(
            SQL_SERVER.getJdbcUrl(), SQL_SERVER.getUsername(), SQL_SERVER.getPassword())) {
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase =
          new Liquibase(
              "db/changelog/db.changelog-master.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        liquibase.update("");
        liquibase.validate();
      }
      try (ResultSet tables =
          connection.getMetaData().getTables(null, null, "DATABASECHANGELOG", null)) {
        assertThat(tables.next()).as("Liquibase's own table exists").isTrue();
      }
    }
  }
}
