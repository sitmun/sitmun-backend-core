package org.sitmun.authorization.proxy.protocols.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JdbcSqlDialectTest {

  @ParameterizedTest
  @CsvSource({
    "jdbc:oracle:thin:@//host:1521/xe,,ORACLE_FETCH",
    "JDBC:ORACLE:OCI:,,ORACLE_FETCH",
    ",oracle.jdbc.OracleDriver,ORACLE_FETCH",
    ",oracle.jdbc.driver.OracleDriver,ORACLE_FETCH",
    "jdbc:postgresql://localhost/db,,LIMIT_OFFSET",
    "jdbc:h2:mem:testdb,,LIMIT_OFFSET",
    "jdbc:h2:file:./data,,LIMIT_OFFSET",
    ",org.postgresql.Driver,LIMIT_OFFSET",
    ",org.h2.Driver,LIMIT_OFFSET",
    ",,LIMIT_OFFSET",
    "jdbc:mysql://localhost/db,,LIMIT_OFFSET",
  })
  @DisplayName("fromJdbcMetadata maps URL and driver to dialect")
  void fromJdbcMetadata(String url, String driver, JdbcSqlDialect expected) {
    assertThat(JdbcSqlDialect.fromJdbcMetadata(driver, url)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "LIMIT_OFFSET, 'SELECT 1', 10, 20, 'SELECT 1 LIMIT 10 OFFSET 20'",
    "LIMIT_OFFSET, 'SELECT 1', 10, , 'SELECT 1 LIMIT 10'",
    "LIMIT_OFFSET, 'SELECT 1', , 5, 'SELECT 1 OFFSET 5'",
    "ORACLE_FETCH, 'SELECT 1', 10, 20, 'SELECT 1 OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY'",
    "ORACLE_FETCH, 'SELECT 1', 10, , 'SELECT 1 FETCH NEXT 10 ROWS ONLY'",
    "ORACLE_FETCH, 'SELECT 1', , 5, 'SELECT 1 OFFSET 5 ROWS'",
  })
  @DisplayName("appendPagination uses dialect-specific syntax")
  void appendPagination(
      JdbcSqlDialect dialect, String sql, String limit, String offset, String expected) {
    assertThat(dialect.appendPagination(sql, limit, offset)).isEqualTo(expected);
  }
}
