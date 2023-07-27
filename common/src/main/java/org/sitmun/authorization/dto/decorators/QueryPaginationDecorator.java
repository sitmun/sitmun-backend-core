package org.sitmun.authorization.dto.decorators;

import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.PayloadDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QueryPaginationDecorator implements Decorator<Map<String, String>> {


  public static final String SQL_LIMIT = "LIMIT";

  public static final String SQL_OFFSET = "OFFSET";

  @Override
  public boolean accept(Map<String, String> target, PayloadDto payload) {
    return payload instanceof DatasourcePayloadDto;
  }

  @Override
  public void addBehavior(Map<String, String> target, PayloadDto payload) {
    DatasourcePayloadDto datasourcePayloadDto = (DatasourcePayloadDto) payload;
    String sql = datasourcePayloadDto.getSql();
    if (sql != null && !sql.isEmpty()) {
      if (target.containsKey(SQL_LIMIT)) {
        sql = sql + " " + SQL_LIMIT + " " + target.get(SQL_LIMIT);
      }
      if (target.containsKey(SQL_OFFSET)) {
        sql = sql + " " + SQL_OFFSET + " " + target.get(SQL_OFFSET);
      }
      datasourcePayloadDto.setSql(sql);
    }
  }
}
