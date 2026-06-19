package org.sitmun.administration.service.i18n;

import java.util.List;
import java.util.Map;

public record LiteralTranslationFilterModel(Map<String, ColumnFilter> columns) {

  public LiteralTranslationFilterModel {
    columns = columns == null ? Map.of() : columns;
  }

  public static LiteralTranslationFilterModel empty() {
    return new LiteralTranslationFilterModel(Map.of());
  }

  public ColumnFilter columnFilter(String column) {
    return columns.get(column);
  }

  public record ColumnFilter(String operator, List<Condition> conditions) {
    public ColumnFilter {
      operator = operator == null ? "AND" : operator.trim();
      conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
  }

  public record Condition(String operator, String value) {
    public Condition {
      operator = operator == null ? null : operator.trim();
      value = value == null ? null : value.trim();
    }
  }
}
