package org.sitmun.authorization.proxy.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed result of effective parameter computation.
 *
 * <p>Represents the final parameter map after merging client-supplied parameters with
 * backend-declared defaults and locked values. Backed by a {@link LinkedHashMap} for stable
 * iteration order.
 *
 * <p>Exposes {@link #asMap()} for compatibility with current decorator signatures.
 *
 * @param parameters effective parameters (LinkedHashMap)
 */
public record EffectiveParameters(LinkedHashMap<String, String> parameters) {

  /**
   * Returns an unmodifiable view of the effective parameter map.
   *
   * <p>Used as escape hatch for current decorator signatures that expect {@code Map<String,
   * String>}.
   *
   * @return unmodifiable map view
   */
  public Map<String, String> asMap() {
    return Collections.unmodifiableMap(parameters);
  }
}
