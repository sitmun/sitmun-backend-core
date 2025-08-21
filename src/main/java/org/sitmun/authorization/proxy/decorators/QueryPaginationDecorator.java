package org.sitmun.authorization.proxy.decorators;

import java.util.Map;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
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
        if (target.containsKey(SQL_LIMIT)) {
          sql = sql + ' ' + SQL_LIMIT + ' ' + target.get(SQL_LIMIT);
        }
        if (target.containsKey(SQL_OFFSET)) {
          sql = sql + ' ' + SQL_OFFSET + ' ' + target.get(SQL_OFFSET);
        }
        jdbcPayloadDto.setSql(sql);
      }
    }
  }
}
