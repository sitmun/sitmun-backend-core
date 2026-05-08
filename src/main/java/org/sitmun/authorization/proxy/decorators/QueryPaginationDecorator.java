package org.sitmun.authorization.proxy.decorators;

import java.util.Map;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcSqlDialect;
import org.springframework.stereotype.Component;

@Component
public class QueryPaginationDecorator implements Decorator<Map<String, String>> {

  public static final String SQL_LIMIT = "LIMIT";

  public static final String SQL_OFFSET = "OFFSET";

  @Override
  public boolean accept(Map<String, String> target, PayloadDto payload) {
    return payload instanceof JdbcPayloadDto;
  }

  @Override
  public void addBehavior(Map<String, String> target, PayloadDto payload) {
    if (payload instanceof JdbcPayloadDto jdbcPayloadDto) {
      String sql = jdbcPayloadDto.getSql();
      if (sql != null && !sql.isEmpty()) {
        String limit = target.get(SQL_LIMIT);
        String offset = target.get(SQL_OFFSET);
        if (limit != null || offset != null) {
          JdbcSqlDialect dialect =
              JdbcSqlDialect.fromJdbcMetadata(jdbcPayloadDto.getDriver(), jdbcPayloadDto.getUri());
          jdbcPayloadDto.setSql(dialect.appendPagination(sql, limit, offset));
        }
      }
    }
  }
}
