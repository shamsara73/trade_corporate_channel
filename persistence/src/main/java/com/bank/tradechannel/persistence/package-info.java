/**
 * JDBC repositories on Spring's JdbcClient, the outbox SQL and the Liquibase changelogs (ADR 0003).
 * SQL is plain and portable; anything specific to SQL Server lives in {@link
 * com.bank.tradechannel.persistence.SqlServerDialect}.
 */
package com.bank.tradechannel.persistence;
