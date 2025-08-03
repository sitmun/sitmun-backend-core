package org.sitmun.authorization.dto.decorators;

import java.util.Map;
import java.util.regex.Pattern;
import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.PayloadDto;
import org.springframework.stereotype.Component;

@Component
public class QueryFixedFiltersDecorator implements Decorator<Map<String, String>> {

  private static final Pattern filterPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

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
    if (payload instanceof DatasourcePayloadDto datasourcePayloadDto) {
      final String[] sql = {datasourcePayloadDto.getSql()};
      if (sql[0] != null && !sql[0].isEmpty()) {
        target.forEach(
            (key, value) -> {
              if (sql[0].contains("${" + key + '}')) {
                sql[0] = sql[0].replace("${" + key + '}', getFilterValue(value));
              }
            });
        datasourcePayloadDto.setSql(sql[0]);
      }
    }
  }
}
