package org.sitmun.authorization.proxy.decorators;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.springframework.stereotype.Component;

@Component
public class QueryVaryFiltersDecorator implements Decorator<Map<String, String>> {

  // ${name} placeholders like in the original code
  private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");
  // Simple numeric detector for unquoted literals
  private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");
  // Case-insensitive WHERE detector
  private static final Pattern HAS_WHERE = Pattern.compile("\\bwhere\\b", Pattern.CASE_INSENSITIVE);

  private static String quoteIfNeeded(String raw) {
    if (raw == null) return "NULL";
    String v = raw.trim();
    if (NUMERIC.matcher(v).matches()) return v;
    // Escape single quotes for SQL string literal
    return "'" + v.replace("'", "''") + "'";
  }

  @Override
  public boolean accept(Map<String, String> target, PayloadDto payload) {
    return payload instanceof JdbcPayloadDto;
  }

  @Override
  public void addBehavior(Map<String, String> target, PayloadDto payload) {
    if (!(payload instanceof JdbcPayloadDto jdbc)) return;
    String sql = jdbc.getSql();
    if (sql == null || sql.isBlank()) return;

    // 1) Replace ${key} placeholders from target (all occurrences), track leftovers.
    Map<String, String> remaining;
    if (target == null || target.isEmpty()) {
      remaining = new LinkedHashMap<>();
    } else {
      remaining = new LinkedHashMap<>(target); // preserve iteration order
    }
    Set<String> matchesKeys = new HashSet<>();
    Matcher m = PLACEHOLDER.matcher(sql);
    StringBuilder sb = new StringBuilder(sql.length());
    while (m.find()) {
      String key = m.group(1);
      String val = remaining.get(key);
      if (val != null) {
        // Safe replacement – avoid interpreting backslashes/$ in the value
        m.appendReplacement(sb, Matcher.quoteReplacement(val));
        matchesKeys.add(key); // may be multiple matches for the same key
      }
    }
    // Remove all matched keys
    matchesKeys.forEach(remaining::remove);
    m.appendTail(sb);
    sql = sb.toString();

    // 2) Append leftover properties as WHERE ... AND ...
    if (!remaining.isEmpty()) {
      StringBuilder out = new StringBuilder(sql.trim());
      if (!HAS_WHERE.matcher(out).find()) {
        // This avoids awkward "AND" placement and malformed SQL endings
        out.append(" WHERE 1=1");
      }
      for (Map.Entry<String, String> e : remaining.entrySet()) {
        // NOTE: keys (column names) are assumed trusted/validated upstream
        out.append(" AND ").append(e.getKey()).append('=').append(quoteIfNeeded(e.getValue()));
      }
      sql = out.toString();
    }

    jdbc.setSql(sql);
  }
}
