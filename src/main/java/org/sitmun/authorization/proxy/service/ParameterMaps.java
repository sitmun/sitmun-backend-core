package org.sitmun.authorization.proxy.service;

import java.util.Map;

/** Writes a query parameter and drops any existing key that differs only by case. */
public final class ParameterMaps {

  private ParameterMaps() {}

  public static void put(Map<String, String> target, String key, String value) {
    if (target == null || key == null) {
      return;
    }
    target.keySet().removeIf(existing -> existing != null && existing.equalsIgnoreCase(key));
    target.put(key, value);
  }
}
