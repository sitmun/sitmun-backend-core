package org.sitmun.authorization.proxy.decorators;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.springframework.stereotype.Component;

@Component
public class SqlUserParametrizationDecorator implements Decorator<Map<String, String>> {

  // Case-insensitive WHERE detector
  private static final Pattern HAS_WHERE = Pattern.compile("\\bwhere\\b", Pattern.CASE_INSENSITIVE);
  // Supports both ${var} and '${var}' patterns to avoid quoted '?' placeholders.
  private static final Pattern SQL_TEMPLATE_VAR = Pattern.compile("'\\$\\{(\\w+)}'|\\$\\{(\\w+)}");

  @Override
  public boolean accept(Map<String, String> target, PayloadDto payload) {
    return payload instanceof JdbcPayloadDto;
  }

  @Override
  public void addBehavior(Map<String, String> target, PayloadDto payload) {
    if (payload instanceof JdbcPayloadDto jdbc) {
      applyJdbcParameterization(target, jdbc);
    }
  }

  private void applyJdbcParameterization(Map<String, String> target, JdbcPayloadDto jdbc) {
    String sql = jdbc.getSql();
    if (sql == null || sql.isBlank()) return;
    if (target == null) return;
    List<String> parameters = new java.util.ArrayList<>();

    // 1) Replace ${key} placeholders from target (all occurrences), track leftovers.
    Map<String, String> remaining =
        target.isEmpty()
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(target); // preserve iteration order

    Set<String> usedVariables = new HashSet<>();
    Matcher matcher = SQL_TEMPLATE_VAR.matcher(sql);
    StringBuilder expandedSql = new StringBuilder(sql.length());
    while (matcher.find()) {
      String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      if (target.containsKey(key)) {
        matcher.appendReplacement(expandedSql, Matcher.quoteReplacement("?"));
        parameters.add(target.get(key));
        usedVariables.add(key);
      }
    }
    matcher.appendTail(expandedSql);
    sql = expandedSql.toString();

    // Remove used variables from remaining
    usedVariables.forEach(remaining::remove);

    // 2) Append leftover properties as WHERE ... AND ...
    if (!remaining.isEmpty()) {
      StringBuilder out = new StringBuilder(sql.trim());
      if (!HAS_WHERE.matcher(out).find()) {
        // This avoids awkward "AND" placement and malformed SQL endings
        out.append(" WHERE 1=1");
      }
      for (Map.Entry<String, String> e : remaining.entrySet()) {
        // NOTE: keys (column names) are assumed trusted/validated upstream
        out.append(" AND ").append(e.getKey()).append("=?");
        parameters.add(e.getValue());
      }
      sql = out.toString();
    }

    jdbc.setSql(sql);
    jdbc.setParameters(parameters);
  }
}
