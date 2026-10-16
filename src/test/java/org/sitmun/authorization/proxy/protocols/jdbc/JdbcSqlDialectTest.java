package org.sitmun.authorization.proxy.protocols.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JdbcSqlDialectTest {

  @ParameterizedTest
  @CsvSource(
      nullValues = "NULL",
      value = {
        "10,20,SELECT 1 OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY",
        "10,NULL,SELECT 1 FETCH NEXT 10 ROWS ONLY",
        "NULL,5,SELECT 1 OFFSET 5 ROWS",
        "NULL,NULL,SELECT 1"
      })
  @DisplayName("appendPagination writes one OFFSET FETCH clause")
  void appendPagination(Integer limit, Integer offset, String expected) {
    assertThat(JdbcSqlDialect.appendPagination("SELECT 1", limit, offset)).isEqualTo(expected);
  }
}
