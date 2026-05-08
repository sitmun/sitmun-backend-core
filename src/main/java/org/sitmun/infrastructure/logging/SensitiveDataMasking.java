package org.sitmun.infrastructure.logging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Redacts sensitive HTTP header names and values for logs (parity with proxy middleware helper).
 */
public final class SensitiveDataMasking {

  public static final String REDACTED = "***";

  private static final List<String> SENSITIVE_KEY_SUBSTRINGS =
      List.of(
          "authorization",
          "proxy-middleware-key",
          "proxy-key",
          "x-api-key",
          "api-key",
          "token",
          "password",
          "secret",
          "set-cookie",
          "cookie");

  private SensitiveDataMasking() {}

  public static boolean isSensitiveKey(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    for (String fragment : SENSITIVE_KEY_SUBSTRINGS) {
      if (lower.contains(fragment)) {
        return true;
      }
    }
    return false;
  }

  public static String maskValue(String headerName, String value) {
    if (value == null) {
      return null;
    }
    if (!isSensitiveKey(headerName)) {
      return value;
    }
    if ("authorization".equals(headerName.trim().toLowerCase(Locale.ROOT))) {
      return maskAuthorizationValue(value);
    }
    return REDACTED;
  }

  static String maskAuthorizationValue(String value) {
    String v = value.trim();
    if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return "Bearer " + REDACTED;
    }
    if (v.regionMatches(true, 0, "Basic ", 0, 6)) {
      return "Basic " + REDACTED;
    }
    return REDACTED;
  }

  public static Map<String, String> maskSortedMap(Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return Map.of();
    }
    TreeMap<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    sorted.putAll(map);
    Map<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<String, String> e : sorted.entrySet()) {
      out.put(e.getKey(), maskValue(e.getKey(), e.getValue()));
    }
    return out;
  }
}
