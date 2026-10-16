package org.sitmun.authorization.proxy.decorators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;

class QueryPaginationDecoratorTest {

  private final QueryPaginationDecorator decorator = new QueryPaginationDecorator();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "jdbc:postgresql://localhost/sitmun",
        "jdbc:h2:mem:test",
        "jdbc:oracle:thin:@//localhost:1521/xe"
      })
  @DisplayName("addBehavior appends the same clause for PostgreSQL, H2, and Oracle")
  void sameClauseForEveryTarget(String uri) {
    JdbcPayloadDto payload = JdbcPayloadDto.builder().uri(uri).sql("SELECT * FROM t").build();

    decorator.addBehavior(
        Map.of(QueryPaginationDecorator.SQL_LIMIT, "10", QueryPaginationDecorator.SQL_OFFSET, "20"),
        payload);

    assertThat(payload.getSql())
        .isEqualTo("SELECT * FROM t OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY");
  }

  @Test
  @DisplayName("addBehavior appends only the part the client sent")
  void appendsOnlyThePartTheClientSent() {
    JdbcPayloadDto limitOnly = JdbcPayloadDto.builder().sql("SELECT * FROM t").build();
    decorator.addBehavior(Map.of(QueryPaginationDecorator.SQL_LIMIT, "5"), limitOnly);
    assertThat(limitOnly.getSql()).isEqualTo("SELECT * FROM t FETCH NEXT 5 ROWS ONLY");

    JdbcPayloadDto offsetOnly = JdbcPayloadDto.builder().sql("SELECT * FROM t").build();
    decorator.addBehavior(Map.of(QueryPaginationDecorator.SQL_OFFSET, "5"), offsetOnly);
    assertThat(offsetOnly.getSql()).isEqualTo("SELECT * FROM t OFFSET 5 ROWS");
  }

  @Test
  @DisplayName("addBehavior leaves SQL unchanged when map has no pagination keys")
  void noPaginationKeysNoOp() {
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT 1").build();

    decorator.addBehavior(Map.of(), payload);

    assertThat(payload.getSql()).isEqualTo("SELECT 1");
  }

  @Test
  @DisplayName("addBehavior rejects a non-integer and leaves the SQL unchanged")
  void rejectsNonInteger() {
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM t").build();

    assertThatThrownBy(
            () ->
                decorator.addBehavior(
                    Map.of(QueryPaginationDecorator.SQL_LIMIT, "10; DROP TABLE t"), payload))
        .isInstanceOf(BadRequestException.class);
    assertThat(payload.getSql()).isEqualTo("SELECT * FROM t");
  }
}
