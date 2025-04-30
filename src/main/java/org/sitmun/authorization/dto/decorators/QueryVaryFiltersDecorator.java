package org.sitmun.authorization.dto.decorators;

import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.PayloadDto;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

@Component
public class QueryVaryFiltersDecorator implements Decorator<Map<String, String>> {

  private static final Pattern filterPattern = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

  private static String getFilterValue(String value) {
    if (!filterPattern.matcher(value).matches()) {
      return '\'' + value + '\'';
    }
    return value;
  }

  @Override
  public boolean accept(Map<String, String> target, PayloadDto payload) {
    return payload instanceof DatasourcePayloadDto;
  }

  @Override
  public void addBehavior(Map<String, String> target, PayloadDto payload) {
    DatasourcePayloadDto datasourcePayloadDto = (DatasourcePayloadDto) payload;
    String sql = datasourcePayloadDto.getSql();
    if (sql != null && !sql.isEmpty() && target != null && !target.isEmpty()) {
      String[] keySetIt = target.keySet().toArray(new String[0]);
      for (String key : keySetIt) {
        if (sql.contains("${" + key + "}")) {
          String value = getFilterValue(target.get(key));
          sql = sql.replace("${" + key + "}", value);
          target.remove(key);
        }
      }
      if (!target.isEmpty()) {
        if (!sql.contains("WHERE") && !sql.contains("where")) {
          sql = sql + " WHERE ";
        } else {
          sql = sql + " AND ";
        }
        StringJoiner joiner = new StringJoiner(" AND ");
        target.forEach((key, value) -> {
          joiner.add(key + '=' + getFilterValue(value));
        });
        sql = sql.concat(joiner.toString());
      }
      datasourcePayloadDto.setSql(sql);
    }
  }
}
