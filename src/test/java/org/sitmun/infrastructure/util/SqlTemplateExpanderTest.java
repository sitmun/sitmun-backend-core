package org.sitmun.infrastructure.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.util.SqlTemplateExpander.ExpandedResult;

class SqlTemplateExpanderTest {

  @Test
  @DisplayName("expand handles simple variable expansion")
  void expandHandlesSimpleVariableExpansion() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId}";
    Map<String, String> params = Map.of("userId", "123");

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals("SELECT * FROM users WHERE id = 123", result);
  }

  @Test
  @DisplayName("expand handles multiple variables")
  void expandHandlesMultipleVariables() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId} AND status = '${status}'";
    Map<String, String> params = Map.of("userId", "123", "status", "active");

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals("SELECT * FROM users WHERE id = 123 AND status = 'active'", result);
  }

  @Test
  @DisplayName("expand handles multiple occurrences of same variable")
  void expandHandlesMultipleOccurrencesOfSameVariable() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${id} OR parent_id = ${id}";
    Map<String, String> params = Map.of("id", "123");

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals("SELECT * FROM users WHERE id = 123 OR parent_id = 123", result);
  }

  @Test
  @DisplayName("expand returns original SQL when parameters are empty")
  void expandReturnsOriginalSqlWhenParametersAreEmpty() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId}";
    Map<String, String> params = Map.of();

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals(sql, result);
  }

  @Test
  @DisplayName("expand returns original SQL when no template variables present")
  void expandReturnsOriginalSqlWhenNoTemplateVariablesPresent() {
    // Given
    String sql = "SELECT * FROM users";
    Map<String, String> params = Map.of("userId", "123");

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals(sql, result);
  }

  @Test
  @DisplayName("expand handles missing variable gracefully")
  void expandHandlesMissingVariableGracefully() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId}";
    Map<String, String> params = Map.of("otherId", "123");

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    // Missing variables are left unexpanded
    assertEquals(sql, result);
  }

  @Test
  @DisplayName("expand throws exception when SQL is null")
  void expandThrowsExceptionWhenSqlIsNull() {
    // Given
    Map<String, String> params = Map.of("userId", "123");

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> SqlTemplateExpander.expand(null, params));
  }

  @Test
  @DisplayName("expand throws exception when parameters are null")
  void expandThrowsExceptionWhenParametersAreNull() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId}";

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> SqlTemplateExpander.expand(sql, null));
  }

  @Test
  @DisplayName("expandWithUsedVariables tracks which variables were used")
  void expandWithUsedVariablesTracksWhichVariablesWereUsed() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId} AND status = '${status}'";
    Map<String, String> params = Map.of("userId", "123", "status", "active", "unused", "value");

    // When
    ExpandedResult result = SqlTemplateExpander.expandWithUsedVariables(sql, params);

    // Then
    assertEquals("SELECT * FROM users WHERE id = 123 AND status = 'active'", result.getSql());
    assertEquals(Set.of("userId", "status"), result.getUsedVariables());
  }

  @Test
  @DisplayName("expandWithUsedVariables returns empty set when no variables used")
  void expandWithUsedVariablesReturnsEmptySetWhenNoVariablesUsed() {
    // Given
    String sql = "SELECT * FROM users";
    Map<String, String> params = Map.of("userId", "123");

    // When
    ExpandedResult result = SqlTemplateExpander.expandWithUsedVariables(sql, params);

    // Then
    assertEquals(sql, result.getSql());
    assertTrue(result.getUsedVariables().isEmpty());
  }

  @Test
  @DisplayName("hasTemplateVariables returns true for valid SQL template")
  void hasTemplateVariablesReturnsTrueForValidSqlTemplate() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId}";

    // When
    boolean result = SqlTemplateExpander.hasTemplateVariables(sql);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("hasTemplateVariables returns false for SQL without variables")
  void hasTemplateVariablesReturnsFalseForSqlWithoutVariables() {
    // Given
    String sql = "SELECT * FROM users";

    // When
    boolean result = SqlTemplateExpander.hasTemplateVariables(sql);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("hasTemplateVariables returns false for null")
  void hasTemplateVariablesReturnsFalseForNull() {
    // When
    boolean result = SqlTemplateExpander.hasTemplateVariables(null);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("hasTemplateVariables returns false for empty string")
  void hasTemplateVariablesReturnsFalseForEmptyString() {
    // When
    boolean result = SqlTemplateExpander.hasTemplateVariables("");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("getVariableNames returns set of variable names")
  void getVariableNamesReturnsSetOfVariableNames() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId} AND status = '${status}'";

    // When
    Set<String> result = SqlTemplateExpander.getVariableNames(sql);

    // Then
    assertEquals(Set.of("userId", "status"), result);
  }

  @Test
  @DisplayName("getVariableNames returns empty set for SQL without variables")
  void getVariableNamesReturnsEmptySetForSqlWithoutVariables() {
    // Given
    String sql = "SELECT * FROM users";

    // When
    Set<String> result = SqlTemplateExpander.getVariableNames(sql);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("getVariableNames throws exception when SQL is null")
  void getVariableNamesThrowsExceptionWhenSqlIsNull() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> SqlTemplateExpander.getVariableNames(null));
  }

  @Test
  @DisplayName("quoteForSql returns NULL for null value")
  void quoteForSqlReturnsNullForNullValue() {
    // When
    String result = SqlTemplateExpander.quoteForSql(null);

    // Then
    assertEquals("NULL", result);
  }

  @Test
  @DisplayName("quoteForSql returns unquoted numeric values")
  void quoteForSqlReturnsUnquotedNumericValues() {
    // When & Then
    assertEquals("123", SqlTemplateExpander.quoteForSql("123"));
    assertEquals("123.456", SqlTemplateExpander.quoteForSql("123.456"));
    assertEquals("-789", SqlTemplateExpander.quoteForSql("-789"));
    assertEquals("1.23e-4", SqlTemplateExpander.quoteForSql("1.23e-4"));
    assertEquals("6.022E23", SqlTemplateExpander.quoteForSql("6.022E23"));
  }

  @Test
  @DisplayName("quoteForSql returns quoted string values")
  void quoteForSqlReturnsQuotedStringValues() {
    // When & Then
    assertEquals("'hello'", SqlTemplateExpander.quoteForSql("hello"));
    assertEquals("'hello world'", SqlTemplateExpander.quoteForSql("hello world"));
    assertEquals("'test@example.com'", SqlTemplateExpander.quoteForSql("test@example.com"));
  }

  @Test
  @DisplayName("quoteForSql escapes single quotes")
  void quoteForSqlEscapesSingleQuotes() {
    // When
    String result = SqlTemplateExpander.quoteForSql("O'Brien");

    // Then
    assertEquals("'O''Brien'", result);
  }

  @Test
  @DisplayName("expandWithQuoting quotes string values automatically")
  void expandWithQuotingQuotesStringValuesAutomatically() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${id} AND name = ${name}";
    Map<String, String> params = Map.of("id", "123", "name", "John Doe");

    // When
    String result = SqlTemplateExpander.expandWithQuoting(sql, params);

    // Then
    assertEquals("SELECT * FROM users WHERE id = 123 AND name = 'John Doe'", result);
  }

  @Test
  @DisplayName("expandWithQuoting handles NULL values")
  void expandWithQuotingHandlesNullValues() {
    // Given
    String sql = "SELECT * FROM users WHERE status IS ${status}";
    Map<String, String> params = new HashMap<>();
    params.put("status", null);

    // When
    String result = SqlTemplateExpander.expandWithQuoting(sql, params);

    // Then
    // expandWithQuoting converts null values to SQL NULL keyword
    assertEquals("SELECT * FROM users WHERE status IS NULL", result);
  }

  @Test
  @DisplayName("expandWithQuoting escapes single quotes in string values")
  void expandWithQuotingEscapesSingleQuotesInStringValues() {
    // Given
    String sql = "SELECT * FROM users WHERE name = ${name}";
    Map<String, String> params = Map.of("name", "O'Brien");

    // When
    String result = SqlTemplateExpander.expandWithQuoting(sql, params);

    // Then
    assertEquals("SELECT * FROM users WHERE name = 'O''Brien'", result);
  }

  @Test
  @DisplayName("expand with mutable map does not modify original")
  void expandWithMutableMapDoesNotModifyOriginal() {
    // Given
    String sql = "SELECT * FROM users WHERE id = ${userId}";
    Map<String, String> params = new HashMap<>();
    params.put("userId", "123");
    params.put("extra", "value");

    // When
    SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals(2, params.size());
    assertTrue(params.containsKey("extra"));
  }

  @Test
  @DisplayName("expand handles special regex characters in values")
  void expandHandlesSpecialRegexCharactersInValues() {
    // Given
    String sql = "SELECT * FROM users WHERE pattern = ${pattern}";
    Map<String, String> params = Map.of("pattern", "$100 & more");

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals("SELECT * FROM users WHERE pattern = $100 & more", result);
  }

  @Test
  @DisplayName("expand handles backslashes in values")
  void expandHandlesBackslashesInValues() {
    // Given
    String sql = "SELECT * FROM files WHERE path = ${path}";
    Map<String, String> params = Map.of("path", "C:\\Users\\test");

    // When
    String result = SqlTemplateExpander.expand(sql, params);

    // Then
    assertEquals("SELECT * FROM files WHERE path = C:\\Users\\test", result);
  }
}
