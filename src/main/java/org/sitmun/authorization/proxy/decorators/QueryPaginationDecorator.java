package org.sitmun.authorization.proxy.decorators;

import java.util.Map;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcSqlDialect;
import org.sitmun.authorization.proxy.service.Pagination;
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
      Integer limit = Pagination.parseCount(target.get(SQL_LIMIT));
      Integer offset = Pagination.parseCount(target.get(SQL_OFFSET));
      String sql = jdbcPayloadDto.getSql();
      if (sql != null && !sql.isEmpty() && (limit != null || offset != null)) {
        jdbcPayloadDto.setSql(JdbcSqlDialect.appendPagination(sql, limit, offset));
      }
    }
  }
}
