package org.sitmun.authorization.proxy.decorators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;

class QueryVaryFiltersDecoratorTest {

  private QueryVaryFiltersDecorator decorator;

  @BeforeEach
  void setUp() {
    decorator = new QueryVaryFiltersDecorator();
  }

  @Test
  @DisplayName("accept returns true for DatasourcePayloadDto")
  void acceptReturnsTrueForDatasourcePayloadDto() {
    // Given
    Map<String, String> target = Map.of();
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM table").build();

    // When
    boolean result = decorator.accept(target, payload);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("addBehavior replaces parameter placeholders in SQL")
  void addBehaviorReplacesParameterPlaceholders() {
    // Given
    Map<String, String> target = Map.of("userId", "123", "status", "active");

    JdbcPayloadDto payload =
        JdbcPayloadDto.builder()
            .sql("SELECT * FROM users WHERE id = ${userId} AND status = '${status}'")
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("SELECT * FROM users WHERE id = ? AND status = ?", payload.getSql());
    assertEquals(2, payload.getParameters().size());
    assertTrue(payload.getParameters().contains("123"));
    assertTrue(payload.getParameters().contains("active"));
    assertEquals(2, target.size()); // Parameters should remain unchanged since target is immutable
  }

  @Test
  @DisplayName("addBehavior adds WHERE clause when SQL has no WHERE")
  void addBehaviorAddsWhereClauseWhenNoWhere() {
    // Given
    Map<String, String> target = Map.of("category", "electronics", "price", "100.50");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE"));
    assertTrue(result.contains("category=?"));
    assertTrue(result.contains("price=?"));
    assertTrue(result.contains(" AND "));
    assertEquals(2, payload.getParameters().size());
    assertTrue(payload.getParameters().contains("electronics"));
    assertTrue(payload.getParameters().contains("100.50"));
    assertEquals(2, target.size()); // Parameters should remain unchanged since target is immutable
  }

  @Test
  @DisplayName("addBehavior adds AND clause when SQL already has WHERE")
  void addBehaviorAddsAndClauseWhenWhereExists() {
    // Given
    Map<String, String> target = Map.of("category", "electronics", "price", "100.50");

    JdbcPayloadDto payload =
        JdbcPayloadDto.builder().sql("SELECT * FROM products WHERE available = true").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE available = true AND"));
    assertTrue(result.contains("category=?"));
    assertTrue(result.contains("price=?"));
    assertEquals(2, payload.getParameters().size());
  }

  @Test
  @DisplayName("addBehavior handles case-insensitive WHERE detection")
  void addBehaviorHandlesCaseInsensitiveWhere() {
    // Given
    Map<String, String> target = Map.of("category", "electronics");

    JdbcPayloadDto payload =
        JdbcPayloadDto.builder().sql("SELECT * FROM products where available = true").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products where available = true AND"));
    assertTrue(result.contains("category=?"));
    assertEquals(1, payload.getParameters().size());
    assertEquals("electronics", payload.getParameters().get(0));
  }

  @Test
  @DisplayName("addBehavior uses parameters for numeric values")
  void addBehaviorUsesParametersForNumericValues() {
    // Given
    Map<String, String> target =
        Map.of("id", "123", "price", "99.99", "quantity", "-5", "rating", "4.5e2");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE"));
    assertTrue(result.contains("id=?"));
    assertTrue(result.contains("price=?"));
    assertTrue(result.contains("quantity=?"));
    assertTrue(result.contains("rating=?"));
    assertEquals(4, payload.getParameters().size());
  }

  @Test
  @DisplayName("addBehavior uses parameters for string values")
  void addBehaviorUsesParametersForStringValues() {
    // Given
    Map<String, String> target =
        Map.of("name", "John Doe", "email", "john@example.com", "status", "active");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM users").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM users WHERE"));
    assertTrue(result.contains("name=?"));
    assertTrue(result.contains("email=?"));
    assertTrue(result.contains("status=?"));
    assertEquals(3, payload.getParameters().size());
  }

  @Test
  @DisplayName("addBehavior handles mixed parameter replacement and WHERE addition")
  void addBehaviorHandlesMixedParameterReplacementAndWhereAddition() {
    // Given
    Map<String, String> target =
        Map.of("userId", "123", "category", "electronics", "price", "100.50");

    JdbcPayloadDto payload =
        JdbcPayloadDto.builder().sql("SELECT * FROM products WHERE user_id = ${userId}").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE user_id = ? AND"));
    assertTrue(result.contains("category=?"));
    assertTrue(result.contains("price=?"));
    assertEquals(3, payload.getParameters().size());
  }

  @Test
  @DisplayName("addBehavior handles null SQL gracefully")
  void addBehaviorHandlesNullSql() {
    // Given
    Map<String, String> target = Map.of("category", "electronics");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql(null).build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertNull(payload.getSql());
    assertTrue(payload.getParameters().isEmpty());
    assertEquals(1, target.size()); // Parameters should remain unchanged
  }

  @Test
  @DisplayName("addBehavior handles empty SQL gracefully")
  void addBehaviorHandlesEmptySql() {
    // Given
    Map<String, String> target = Map.of("category", "electronics");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("", payload.getSql());
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName("addBehavior handles null target gracefully")
  void addBehaviorHandlesNullTarget() {
    // Given
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(null, payload);

    // Then
    assertEquals("SELECT * FROM products", payload.getSql()); // SQL should remain unchanged
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName("addBehavior handles empty target gracefully")
  void addBehaviorHandlesEmptyTarget() {
    // Given
    Map<String, String> target = Map.of();
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("SELECT * FROM products", payload.getSql()); // SQL should remain unchanged
    assertTrue(payload.getParameters().isEmpty());
  }

  @Test
  @DisplayName("addBehavior handles null payload gracefully")
  void addBehaviorHandlesNullPayload() {
    // Given
    Map<String, String> target = Map.of("category", "electronics");

    // When & Then
    assertDoesNotThrow(() -> decorator.addBehavior(target, null));
    assertEquals(1, target.size()); // Parameters should remain unchanged
  }

  @Test
  @DisplayName("addBehavior handles complex numeric patterns")
  void addBehaviorHandlesComplexNumericPatterns() {
    // Given
    Map<String, String> target =
        Map.of(
            "intValue", "42",
            "floatValue", "3.14159",
            "negativeValue", "-123.456",
            "scientificValue", "1.23e-4",
            "scientificValue2", "6.022E23");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM measurements").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM measurements WHERE"));
    assertTrue(result.contains("intValue=?"));
    assertTrue(result.contains("floatValue=?"));
    assertTrue(result.contains("negativeValue=?"));
    assertTrue(result.contains("scientificValue=?"));
    assertTrue(result.contains("scientificValue2=?"));
    assertEquals(5, payload.getParameters().size());
  }

  @Test
  @DisplayName("addBehavior handles string values that look like numbers")
  void addBehaviorHandlesStringValuesThatLookLikeNumbers() {
    // Given
    Map<String, String> target =
        Map.of("phone", "123-456-7890", "zipCode", "12345", "partNumber", "ABC123");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM customers").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM customers WHERE"));
    assertTrue(result.contains("phone=?"));
    assertTrue(result.contains("zipCode=?"));
    assertTrue(result.contains("partNumber=?"));
    assertEquals(3, payload.getParameters().size());
  }

  @Test
  @DisplayName("addBehavior processes all parameters")
  void addBehaviorProcessesAllParameters() {
    // Given
    Map<String, String> target = Map.of("z", "last", "a", "first", "m", "middle");

    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM items").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM items WHERE"));
    assertTrue(result.contains("a=?"));
    assertTrue(result.contains("m=?"));
    assertTrue(result.contains("z=?"));
    assertEquals(3, payload.getParameters().size());
  }

  @Test
  @DisplayName("addBehavior preserves parameter order for linked maps")
  void addBehaviorPreservesParameterOrderForLinkedMaps() {
    // Given
    Map<String, String> target = new java.util.LinkedHashMap<>();
    target.put("first", "1");
    target.put("second", "2");
    target.put("third", "3");
    JdbcPayloadDto payload = JdbcPayloadDto.builder().sql("SELECT * FROM t").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals(
        "SELECT * FROM t WHERE 1=1 AND first=? AND second=? AND third=?", payload.getSql());
    assertEquals(java.util.List.of("1", "2", "3"), payload.getParameters());
  }

  @Test
  @DisplayName("addBehavior replaces parameter placeholders in HTTP URL")
  void addBehaviorReplacesParameterPlaceholdersInHttpUrl() {
    // Given
    Map<String, String> target = Map.of("userId", "123", "action", "search");

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/users/{userId}/{action}")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/users/123/search", payload.getUri());
  }

  @Test
  @DisplayName("addBehavior handles HTTP URL with multiple occurrences of same parameter")
  void addBehaviorHandlesMultipleOccurrencesInHttpUrl() {
    // Given
    Map<String, String> target = Map.of("id", "42");

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/item/{id}/related/{id}")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/item/42/related/42", payload.getUri());
  }

  @Test
  @DisplayName("addBehavior handles null target for HTTP URL gracefully")
  void addBehaviorHandlesNullTargetForHttpUrl() {
    // Given
    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/endpoint")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(null, payload);

    // Then
    assertEquals("https://api.example.com/endpoint", payload.getUri());
  }

  @Test
  @DisplayName("addBehavior handles empty target for HTTP URL gracefully")
  void addBehaviorHandlesEmptyTargetForHttpUrl() {
    // Given
    Map<String, String> target = Map.of();
    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/endpoint")
            .method("GET")
            .parameters(new HashMap<>())
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/endpoint", payload.getUri());
  }

  // Defect test for parameter cleanup
  @Test
  @DisplayName(
      "addBehavior removes plain key from parameters after inlining in URL (expected to fail before fix)")
  void addBehaviorRemovesPlainKeyFromParametersAfterInlining() {
    // Given
    Map<String, String> target = new HashMap<>();
    target.put("userId", "123");
    target.put("action", "search");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("userId", "123");
    parameters.put("action", "search");

    WmsPayloadDto payload =
        WmsPayloadDto.builder()
            .uri("https://api.example.com/users/{userId}/{action}")
            .method("GET")
            .parameters(parameters)
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("https://api.example.com/users/123/search", payload.getUri());
    // After inlining, the plain keys should be removed from parameters
    assertFalse(payload.getParameters().containsKey("userId"));
    assertFalse(payload.getParameters().containsKey("action"));
  }
}
