package org.sitmun.administration.service.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TemplateContextNormalizer {

  private static final String VALUE = "value";
  private static final Pattern FLATTENED_ROW_FIELD_PATTERN =
      Pattern.compile("^(?:items|rows)\\[(\\d+)](?:\\.(.+))?$");
  private static final Pattern FIELD_PATH_SEGMENT_PATTERN =
      Pattern.compile("[^.\\[\\]]+(?:\\[\\d+])?");
  private static final Pattern FIELD_PATH_ARRAY_SEGMENT_PATTERN =
      Pattern.compile("^([^\\[]+)(?:\\[(\\d+)])?$");

  public Map<String, Object> normalize(Map<String, Object> context) {
    if (context == null || context.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Object> normalized = new LinkedHashMap<>();
    context.forEach((key, value) -> normalized.put(key, normalizeValue(value)));
    return normalized;
  }

  private Object normalizeValue(Object value) {
    if (value instanceof Map<?, ?> mapValue) {
      return normalizeMap(mapValue);
    }
    if (value instanceof List<?> listValue) {
      return listValue.stream().map(this::normalizeValue).toList();
    }
    return value;
  }

  private Map<String, Object> normalizeMap(Map<?, ?> mapValue) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    mapValue.forEach((key, value) -> normalized.put(String.valueOf(key), normalizeValue(value)));

    if (!normalized.containsKey("rows") && normalized.get("items") instanceof List<?> items) {
      normalized.put("rows", items.stream().map(this::normalizeValue).toList());
    }

    Object rows = normalized.get("rows");
    if (rows instanceof List<?> rowList) {
      List<Object> normalizedRows = normalizeRows(rowList);
      normalized.put("rows", normalizedRows);
      exposeFirstRowFields(normalized, normalizedRows);
    }

    return normalized;
  }

  private List<Object> normalizeRows(List<?> rows) {
    if (rows.isEmpty()) {
      return List.of();
    }
    if (!rows.stream().allMatch(this::isFlattenedFieldValueRow)) {
      return rows.stream().map(this::normalizeValue).toList();
    }

    Map<Integer, Map<String, Object>> rowMap = new LinkedHashMap<>();
    for (Object rawRow : rows) {
      Map<?, ?> flattenedRow = (Map<?, ?>) rawRow;
      Matcher matcher =
          FLATTENED_ROW_FIELD_PATTERN.matcher(String.valueOf(flattenedRow.get("field")));
      if (!matcher.matches()) {
        return rows.stream().map(this::normalizeValue).toList();
      }

      int rowIndex = Integer.parseInt(matcher.group(1));
      String fieldPath = matcher.group(2);
      Map<String, Object> normalizedRow =
          rowMap.computeIfAbsent(rowIndex, ignored -> new LinkedHashMap<>());
      if (fieldPath == null || fieldPath.isBlank()) {
        normalizedRow.put(VALUE, normalizeValue(flattenedRow.get(VALUE)));
      } else {
        assignPathValue(normalizedRow, fieldPath, normalizeValue(flattenedRow.get(VALUE)));
      }
    }

    return rowMap.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> (Object) entry.getValue())
        .toList();
  }

  private boolean isFlattenedFieldValueRow(Object row) {
    if (!(row instanceof Map<?, ?> rowMap)) {
      return false;
    }
    return rowMap.get("field") instanceof String && rowMap.containsKey(VALUE);
  }

  @SuppressWarnings("unchecked")
  private void assignPathValue(Map<String, Object> root, String path, Object value) {
    Matcher segmentMatcher = FIELD_PATH_SEGMENT_PATTERN.matcher(path);
    List<String> segments = new ArrayList<>();
    while (segmentMatcher.find()) {
      segments.add(segmentMatcher.group());
    }

    Map<String, Object> target = root;
    for (int index = 0; index < segments.size(); index++) {
      String segment = segments.get(index);
      Matcher arraySegmentMatcher = FIELD_PATH_ARRAY_SEGMENT_PATTERN.matcher(segment);
      if (!arraySegmentMatcher.matches()) {
        target.put(segment, value);
        return;
      }

      String propertyName = arraySegmentMatcher.group(1);
      String arrayIndex = arraySegmentMatcher.group(2);
      boolean lastSegment = index == segments.size() - 1;

      if (arrayIndex == null) {
        if (lastSegment) {
          target.put(propertyName, value);
          return;
        }

        Object nextValue = target.get(propertyName);
        if (!(nextValue instanceof Map<?, ?>)) {
          nextValue = new LinkedHashMap<String, Object>();
          target.put(propertyName, nextValue);
        }
        target = (Map<String, Object>) nextValue;
        continue;
      }

      int numericIndex = Integer.parseInt(arrayIndex);
      Object arrayValue = target.get(propertyName);
      if (!(arrayValue instanceof List<?>)) {
        arrayValue = new ArrayList<>();
        target.put(propertyName, arrayValue);
      }

      List<Object> listValue = (List<Object>) arrayValue;
      while (listValue.size() <= numericIndex) {
        listValue.add(null);
      }
      if (lastSegment) {
        listValue.set(numericIndex, value);
        return;
      }

      Object nextValue = listValue.get(numericIndex);
      if (!(nextValue instanceof Map<?, ?>)) {
        nextValue = new LinkedHashMap<String, Object>();
        listValue.set(numericIndex, nextValue);
      }
      target = (Map<String, Object>) nextValue;
    }
  }

  private void exposeFirstRowFields(Map<String, Object> context, List<Object> rows) {
    if (rows.isEmpty() || !(rows.get(0) instanceof Map<?, ?> firstRow)) {
      return;
    }

    firstRow.forEach((key, value) -> context.putIfAbsent(String.valueOf(key), value));
  }
}
