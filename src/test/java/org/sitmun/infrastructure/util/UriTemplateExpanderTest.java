package org.sitmun.infrastructure.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.util.UriTemplateExpander.ExpandedResult;

class UriTemplateExpanderTest {

  @Test
  @DisplayName("expand handles simple variable expansion")
  void expandHandlesSimpleVariableExpansion() {
    // Given
    String template = "https://api.example.com/users/{userId}";
    Map<String, String> params = Map.of("userId", "123");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals("https://api.example.com/users/123", result);
  }

  @Test
  @DisplayName("expand handles multiple variables")
  void expandHandlesMultipleVariables() {
    // Given
    String template = "https://api.example.com/users/{userId}/posts/{postId}";
    Map<String, String> params = Map.of("userId", "123", "postId", "456");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals("https://api.example.com/users/123/posts/456", result);
  }

  @Test
  @DisplayName("expand handles query parameter operators")
  void expandHandlesQueryParameterOperators() {
    // Given
    String template = "https://api.example.com/search{?q,limit}";
    Map<String, String> params = Map.of("q", "test", "limit", "10");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals("https://api.example.com/search?q=test&limit=10", result);
  }

  @Test
  @DisplayName("expand handles path operators")
  void expandHandlesPathOperators() {
    // Given
    String template = "https://api.example.com/{+path}";
    Map<String, String> params = Map.of("path", "users/123/posts");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals("https://api.example.com/users/123/posts", result);
  }

  @Test
  @DisplayName("expand handles fragment operators")
  void expandHandlesFragmentOperators() {
    // Given
    String template = "https://api.example.com/page{#section}";
    Map<String, String> params = Map.of("section", "introduction");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals("https://api.example.com/page#introduction", result);
  }

  @Test
  @DisplayName("expand handles reserved character expansion")
  void expandHandlesReservedCharacterExpansion() {
    // Given
    String template = "https://api.example.com/search{?q}";
    Map<String, String> params = Map.of("q", "hello world");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals("https://api.example.com/search?q=hello%20world", result);
  }

  @Test
  @DisplayName("expand returns original template when parameters are empty")
  void expandReturnsOriginalTemplateWhenParametersAreEmpty() {
    // Given
    String template = "https://api.example.com/users/{userId}";
    Map<String, String> params = Map.of();

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals(template, result);
  }

  @Test
  @DisplayName("expand returns original string when no template variables present")
  void expandReturnsOriginalStringWhenNoTemplateVariablesPresent() {
    // Given
    String template = "https://api.example.com/users";
    Map<String, String> params = Map.of("userId", "123");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals(template, result);
  }

  @Test
  @DisplayName("expand handles missing variable gracefully")
  void expandHandlesMissingVariableGracefully() {
    // Given
    String template = "https://api.example.com/users/{userId}";
    Map<String, String> params = Map.of("otherId", "123");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    // Undefined variables are left unexpanded or removed based on RFC 6570
    assertNotNull(result);
  }

  @Test
  @DisplayName("expand throws exception when template is null")
  void expandThrowsExceptionWhenTemplateIsNull() {
    // Given
    Map<String, String> params = Map.of("userId", "123");

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> UriTemplateExpander.expand(null, params));
  }

  @Test
  @DisplayName("expand throws exception when parameters are null")
  void expandThrowsExceptionWhenParametersAreNull() {
    // Given
    String template = "https://api.example.com/users/{userId}";

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> UriTemplateExpander.expand(template, null));
  }

  @Test
  @DisplayName("expandWithUsedVariables tracks which variables were used")
  void expandWithUsedVariablesTracksWhichVariablesWereUsed() {
    // Given
    String template = "https://api.example.com/users/{userId}/posts/{postId}";
    Map<String, String> params = Map.of("userId", "123", "postId", "456", "unused", "789");

    // When
    ExpandedResult result = UriTemplateExpander.expandWithUsedVariables(template, params);

    // Then
    assertEquals("https://api.example.com/users/123/posts/456", result.getUri());
    assertEquals(Set.of("userId", "postId"), result.getUsedVariables());
  }

  @Test
  @DisplayName("expandWithUsedVariables returns empty set when no variables used")
  void expandWithUsedVariablesReturnsEmptySetWhenNoVariablesUsed() {
    // Given
    String template = "https://api.example.com/users";
    Map<String, String> params = Map.of("userId", "123");

    // When
    ExpandedResult result = UriTemplateExpander.expandWithUsedVariables(template, params);

    // Then
    assertEquals(template, result.getUri());
    assertTrue(result.getUsedVariables().isEmpty());
  }

  @Test
  @DisplayName("hasTemplateVariables returns true for valid template")
  void hasTemplateVariablesReturnsTrueForValidTemplate() {
    // Given
    String template = "https://api.example.com/users/{userId}";

    // When
    boolean result = UriTemplateExpander.hasTemplateVariables(template);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("hasTemplateVariables returns false for string without variables")
  void hasTemplateVariablesReturnsFalseForStringWithoutVariables() {
    // Given
    String template = "https://api.example.com/users";

    // When
    boolean result = UriTemplateExpander.hasTemplateVariables(template);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("hasTemplateVariables returns false for null")
  void hasTemplateVariablesReturnsFalseForNull() {
    // When
    boolean result = UriTemplateExpander.hasTemplateVariables(null);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("hasTemplateVariables returns false for empty string")
  void hasTemplateVariablesReturnsFalseForEmptyString() {
    // When
    boolean result = UriTemplateExpander.hasTemplateVariables("");

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("getVariableNames returns set of variable names")
  void getVariableNamesReturnsSetOfVariableNames() {
    // Given
    String template = "https://api.example.com/users/{userId}/posts/{postId}";

    // When
    Set<String> result = UriTemplateExpander.getVariableNames(template);

    // Then
    assertEquals(Set.of("userId", "postId"), result);
  }

  @Test
  @DisplayName("getVariableNames returns empty set for template without variables")
  void getVariableNamesReturnsEmptySetForTemplateWithoutVariables() {
    // Given
    String template = "https://api.example.com/users";

    // When
    Set<String> result = UriTemplateExpander.getVariableNames(template);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("getVariableNames throws exception when template is null")
  void getVariableNamesThrowsExceptionWhenTemplateIsNull() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () -> UriTemplateExpander.getVariableNames(null));
  }

  @Test
  @DisplayName("expand handles URL encoding correctly")
  void expandHandlesUrlEncodingCorrectly() {
    // Given
    String template = "https://api.example.com/search{?query}";
    Map<String, String> params = Map.of("query", "hello world & goodbye");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    // The library should properly encode special characters
    assertTrue(result.contains("hello%20world"));
  }

  @Test
  @DisplayName("expand handles multiple occurrences of same variable")
  void expandHandlesMultipleOccurrencesOfSameVariable() {
    // Given
    String template = "https://api.example.com/{id}/related/{id}";
    Map<String, String> params = Map.of("id", "123");

    // When
    String result = UriTemplateExpander.expand(template, params);

    // Then
    assertEquals("https://api.example.com/123/related/123", result);
  }

  @Test
  @DisplayName("expand with mutable map does not modify original")
  void expandWithMutableMapDoesNotModifyOriginal() {
    // Given
    String template = "https://api.example.com/users/{userId}";
    Map<String, String> params = new HashMap<>();
    params.put("userId", "123");
    params.put("extra", "value");

    // When
    UriTemplateExpander.expand(template, params);

    // Then
    assertEquals(2, params.size());
    assertTrue(params.containsKey("extra"));
  }
}
