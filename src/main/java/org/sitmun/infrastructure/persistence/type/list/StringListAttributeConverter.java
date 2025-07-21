package org.sitmun.infrastructure.persistence.type.list;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps a {@link List} of {@link String}s separated by comma to a String.
 * Caveats:
 * <ul>
 *  <li><code>null</code> values in columns are mapped to <code>null</code> lists.</li>
 *  <li>A blank or empty string value in a columns is always interpreted as an empty list.</li>
 *  <li><code>null</code> values in lists are preserved with the literal <code>null</code></li>.
 * </ul>
 */
@Converter
public class StringListAttributeConverter implements AttributeConverter<List<String>, String> {

  public static final String DELIMITER = ",";

  @Override
  public String convertToDatabaseColumn(List<String> list) {
    if (list == null) {
      return null;
    }
    if (list.isEmpty()) {
      return "";
    }
    return list.stream().map(it -> {
      if (it == null) {
        return "null";
      }
      return it.trim();
    }).collect(Collectors.joining(DELIMITER));
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    if (dbData.trim().isEmpty()) {
      return Collections.emptyList();
    }
    return Arrays.stream(dbData.split(DELIMITER))
      .map(String::trim)
      .map(it -> {
        if ("null".equals(it)) {
          return null;
        }
        return it;
      }).collect(Collectors.toList());
  }
}