package org.sitmun.authorization.proxy.decorators;

import java.util.Map;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.infrastructure.util.SqlTemplateExpander;
import org.springframework.stereotype.Component;

@Component
public class QueryFixedFiltersDecorator implements Decorator<Map<String, String>> {

  @Override
  public boolean accept(Map<String, String> target, PayloadDto payload) {
    return payload instanceof JdbcPayloadDto;
  }

  @Override
  public void addBehavior(Map<String, String> target, PayloadDto payload) {
    if (payload instanceof JdbcPayloadDto jdbcPayloadDto) {
      final String sql = jdbcPayloadDto.getSql();
      if (sql != null && !sql.isEmpty()) {
        String expandedSql = SqlTemplateExpander.expandWithQuoting(sql, target);
        jdbcPayloadDto.setSql(expandedSql);
      }
    }
  }
}
