package org.sitmun.authorization.proxy.parameter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.sitmun.authorization.proxy.ogc.OgcRequestNames.Operation;
import org.sitmun.authorization.proxy.service.ParameterMaps;

public final class ParameterScopes {

  public enum Kind {
    PROTOCOL,
    OPERATION,
    PROFILE,
    CLIENT,
    CACHE_VARY
  }

  public record Rule(Kind kind, Optional<Operation> operation) {
    static Rule protocol() {
      return new Rule(Kind.PROTOCOL, Optional.empty());
    }

    static Rule operation(Operation operation) {
      return new Rule(Kind.OPERATION, Optional.of(operation));
    }

    static Rule profile() {
      return new Rule(Kind.PROFILE, Optional.empty());
    }

    static Rule client() {
      return new Rule(Kind.CLIENT, Optional.empty());
    }

    static Rule vary() {
      return new Rule(Kind.CACHE_VARY, Optional.empty());
    }
  }

  private static final Map<String, Map<String, Rule>> BY_SERVICE_TYPE =
      Map.of(
          "WMS",
          Map.of(
              "WMS", Rule.protocol(),
              "GETMAP", Rule.operation(Operation.GET_MAP),
              "INFO", Rule.operation(Operation.GET_FEATURE_INFO),
              "OLPARAM", Rule.client(),
              "VARY", Rule.vary()),
          "WMTS",
          Map.of(
              "WMTS", Rule.profile(),
              "OLPARAM", Rule.client(),
              "VARY", Rule.vary()),
          "WFS",
          Map.of(
              "WFS", Rule.protocol(),
              "OLPARAM", Rule.client(),
              "VARY", Rule.vary()));

  private ParameterScopes() {}

  public static Rule of(String serviceType, String code) {
    Map<String, Rule> rules = BY_SERVICE_TYPE.get(normalize(serviceType));
    if (rules == null || code == null) {
      return null;
    }
    return rules.get(normalize(code));
  }

  public static boolean allowed(String serviceType, String code) {
    return of(serviceType, code) != null;
  }

  public static boolean publishedOnService(String serviceType, String code) {
    Rule rule = of(serviceType, code);
    return rule != null && (rule.kind() == Kind.PROTOCOL || rule.kind() == Kind.PROFILE);
  }

  public static boolean publishedOnLayer(String serviceType, String code) {
    Rule rule = of(serviceType, code);
    return rule != null && rule.kind() == Kind.PROTOCOL;
  }

  public static Map<String, String> fixedParameters(
      String serviceType,
      Operation operation,
      Collection<NamedParameter> serviceRows,
      Collection<NamedParameter> layerRows) {
    Map<String, String> fixed = new LinkedHashMap<>();
    apply(fixed, serviceType, operation, serviceRows, Kind.PROTOCOL);
    apply(fixed, serviceType, operation, serviceRows, Kind.OPERATION);
    apply(fixed, serviceType, operation, layerRows, Kind.PROTOCOL);
    apply(fixed, serviceType, operation, layerRows, Kind.OPERATION);
    return fixed;
  }

  private static void apply(
      Map<String, String> target,
      String serviceType,
      Operation operation,
      Collection<NamedParameter> rows,
      Kind kind) {
    if (rows == null) {
      return;
    }
    for (NamedParameter row : rows) {
      Rule rule = of(serviceType, row.type());
      if (rule == null || rule.kind() != kind) {
        continue;
      }
      if (kind == Kind.OPERATION
          && (operation == null || rule.operation().filter(operation::equals).isEmpty())) {
        continue;
      }
      ParameterMaps.put(target, row.name(), row.value());
    }
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
