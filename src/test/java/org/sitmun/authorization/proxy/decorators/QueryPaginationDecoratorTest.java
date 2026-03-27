package org.sitmun.authorization.proxy.decorators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;

class QueryPaginationDecoratorTest {

  private final QueryPaginationDecorator decorator = new QueryPaginationDecorator();

  @Test
  @DisplayName("addBehavior appends LIMIT OFFSET for PostgreSQL URL")
  void postgresqlUrlUsesLimitOffset() {
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .uri("jdbc:postgresql://localhost/sitmun")
            .driver("org.postgresql.Driver")
            .sql("SELECT * FROM t")
            .build();
    Map<String, String> target = new HashMap<>();
    target.put(QueryPaginationDecorator.SQL_LIMIT, "10");
    target.put(QueryPaginationDecorator.SQL_OFFSET, "20");

    decorator.addBehavior(target, payload);

    assertThat(payload.getSql()).isEqualTo("SELECT * FROM t LIMIT 10 OFFSET 20");
  }

  @Test
  @DisplayName("addBehavior appends LIMIT OFFSET for H2 URL")
  void h2UrlUsesLimitOffset() {
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder().uri("jdbc:h2:mem:test").sql("SELECT * FROM t").build();
    Map<String, String> target = Map.of(QueryPaginationDecorator.SQL_LIMIT, "5");

    decorator.addBehavior(target, payload);

    assertThat(payload.getSql()).isEqualTo("SELECT * FROM t LIMIT 5");
  }

  @Test
  @DisplayName("addBehavior appends Oracle FETCH for Oracle URL")
  void oracleUrlUsesFetch() {
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .uri("jdbc:oracle:thin:@//localhost:1521/xe")
            .sql("SELECT * FROM t")
            .build();
    Map<String, String> target = new HashMap<>();
    target.put(QueryPaginationDecorator.SQL_LIMIT, "10");
    target.put(QueryPaginationDecorator.SQL_OFFSET, "20");

    decorator.addBehavior(target, payload);

    assertThat(payload.getSql())
        .isEqualTo("SELECT * FROM t OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY");
  }

  @Test
  @DisplayName("addBehavior uses Oracle syntax when driver class indicates Oracle")
  void oracleDriverUsesFetch() {
    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .uri("")
            .driver("oracle.jdbc.OracleDriver")
            .sql("SELECT 1 FROM dual")
            .build();
    Map<String, String> target = Map.of(QueryPaginationDecorator.SQL_LIMIT, "1");

    decorator.addBehavior(target, payload);

    assertThat(payload.getSql()).isEqualTo("SELECT 1 FROM dual FETCH NEXT 1 ROWS ONLY");
  }

  @Test
  @DisplayName("addBehavior leaves SQL unchanged when map has no pagination keys")
  void noPaginationKeysNoOp() {
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT 1").build();

    decorator.addBehavior(Map.of(), payload);

    assertThat(payload.getSql()).isEqualTo("SELECT 1");
  }
}
