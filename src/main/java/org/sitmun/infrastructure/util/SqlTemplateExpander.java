package org.sitmun.infrastructure.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

/**
 * Utility class for expanding SQL templates. ONLY handles ${variable} syntax for SQL queries. This
 * is separate from URI templates to maintain SQL-specific behavior.
 *
 * <p>SQL templates use ${variable} syntax which is standard for SQL parameter substitution. This
 * class handles proper quoting and escaping of values for SQL contexts.
 */
public final class SqlTemplateExpander {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)}");
  private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?$");

  private SqlTemplateExpander() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Expands a SQL template with the given parameters. Uses ${variable} syntax which is standard for
   * SQL templates.
   *
   * <p>Note: Parameters with null values are skipped (placeholder remains unexpanded). For explicit
   * NULL handling in SQL, use expandWithQuoting which converts null to SQL NULL keyword.
   *
   * @param sql The SQL template string with ${variable} syntax
   * @param parameters Map of parameter names to values
   * @return Expanded SQL string
   * @throws IllegalArgumentException if sql or parameters are null
   */
  public static String expand(String sql, Map<String, String> parameters) {
    if (sql == null) {
      throw new IllegalArgumentException("SQL template cannot be null");
    }
    if (parameters == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }

    if (parameters.isEmpty() || !hasTemplateVariables(sql)) {
      return sql;
    }

    Matcher matcher = PLACEHOLDER.matcher(sql);
    StringBuilder result = new StringBuilder(sql.length());

    while (matcher.find()) {
      String key = matcher.group(1);
      String value = parameters.get(key);
      // Skip null values - leave placeholder unexpanded
      if (value != null) {
        // Safe replacement – avoid interpreting backslashes/$ in the value
        matcher.appendReplacement(result, Matcher.quoteReplacement(value));
      }
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * Expands template and returns both the expanded string and set of variables that were
   * successfully expanded.
   *
   * @param sql The SQL template string
   * @param parameters Map of parameter names to values
   * @return ExpandedResult containing SQL and used variable names
   * @throws IllegalArgumentException if sql or parameters are null
   */
  public static ExpandedResult expandWithUsedVariables(String sql, Map<String, String> parameters) {
    if (sql == null) {
      throw new IllegalArgumentException("SQL template cannot be null");
    }
    if (parameters == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }

    if (parameters.isEmpty() || !hasTemplateVariables(sql)) {
      return new ExpandedResult(sql, Collections.emptySet());
    }

    Set<String> usedVars = new HashSet<>();
    Matcher matcher = PLACEHOLDER.matcher(sql);
    StringBuilder result = new StringBuilder(sql.length());

    while (matcher.find()) {
      String key = matcher.group(1);
      String value = parameters.get(key);
      if (value != null) {
        matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        usedVars.add(key);
      }
    }
    matcher.appendTail(result);

    return new ExpandedResult(result.toString(), usedVars);
  }

  /**
   * Checks if a string contains SQL template variables ${variable}.
   *
   * @param sql String to check
   * @return true if contains ${variable} patterns
   */
  public static boolean hasTemplateVariables(String sql) {
    if (sql == null || sql.isEmpty()) {
      return false;
    }
    return sql.contains("${") && sql.contains("}");
  }

  /**
   * Gets the list of variable names present in a SQL template.
   *
   * @param sql The SQL template string
   * @return Set of variable names found in the template
   * @throws IllegalArgumentException if sql is null
   */
  public static Set<String> getVariableNames(String sql) {
    if (sql == null) {
      throw new IllegalArgumentException("SQL template cannot be null");
    }

    if (!hasTemplateVariables(sql)) {
      return Collections.emptySet();
    }

    Set<String> variables = new HashSet<>();
    Matcher matcher = PLACEHOLDER.matcher(sql);
    while (matcher.find()) {
      variables.add(matcher.group(1));
    }
    return variables;
  }

  /**
   * Quotes a value appropriately for SQL (detects numbers vs strings). Numeric values are returned
   * as-is. String values are single-quoted with proper escaping. NULL is returned for null values.
   *
   * @param value The value to quote
   * @return Quoted value for SQL
   */
  public static String quoteForSql(String value) {
    if (value == null) {
      return "NULL";
    }
    String trimmed = value.trim();
    if (NUMERIC.matcher(trimmed).matches()) {
      return trimmed;
    }
    // Escape single quotes for SQL string literal
    return "'" + trimmed.replace("'", "''") + "'";
  }

  /**
   * Expands SQL template with quoted values. This method automatically quotes string values and
   * leaves numeric values unquoted. NULL values are converted to SQL NULL keyword.
   *
   * <p>IMPORTANT: When using NULL values, your SQL template should be structured to handle them
   * properly. For example:
   *
   * <ul>
   *   <li>Good: "WHERE status IS ${status}" (works with NULL)
   *   <li>Bad: "WHERE status = ${status}" (= NULL doesn't work in SQL)
   * </ul>
   *
   * @param sql The SQL template string with ${variable} syntax
   * @param parameters Map of parameter names to values (null values become SQL NULL)
   * @return Expanded SQL string with properly quoted values
   * @throws IllegalArgumentException if sql or parameters are null
   */
  public static String expandWithQuoting(String sql, Map<String, String> parameters) {
    if (sql == null) {
      throw new IllegalArgumentException("SQL template cannot be null");
    }
    if (parameters == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }

    if (parameters.isEmpty() || !hasTemplateVariables(sql)) {
      return sql;
    }

    Matcher matcher = PLACEHOLDER.matcher(sql);
    StringBuilder result = new StringBuilder(sql.length());

    while (matcher.find()) {
      String key = matcher.group(1);
      // Check if key exists in map (handles null values explicitly)
      if (parameters.containsKey(key)) {
        String value = parameters.get(key);
        String quotedValue = quoteForSql(value);
        matcher.appendReplacement(result, Matcher.quoteReplacement(quotedValue));
      }
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /** Result of template expansion with tracking of which variables were used. */
  @Value
  public static class ExpandedResult {
    String sql;
    Set<String> usedVariables;
  }
}
