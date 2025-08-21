package org.sitmun.authorization.dto.decorators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.dto.DatasourcePayloadDto;
import org.sitmun.authorization.dto.OgcWmsPayloadDto;

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
    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM table").build();

    // When
    boolean result = decorator.accept(target, payload);

    // Then
    assertTrue(result);
  }

  @Test
  @DisplayName("accept returns false for non-DatasourcePayloadDto")
  void acceptReturnsFalseForNonDatasourcePayloadDto() {
    // Given
    Map<String, String> target = Map.of();
    OgcWmsPayloadDto payload =
        OgcWmsPayloadDto.builder().uri("https://example.com").method("GET").build();

    // When
    boolean result = decorator.accept(target, payload);

    // Then
    assertFalse(result);
  }

  @Test
  @DisplayName("addBehavior replaces parameter placeholders in SQL")
  void addBehaviorReplacesParameterPlaceholders() {
    // Given
    Map<String, String> target = Map.of("userId", "123", "status", "active");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder()
            .sql("SELECT * FROM users WHERE id = ${userId} AND status = ${status}")
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("SELECT * FROM users WHERE id = 123 AND status = 'active'", payload.getSql());
    assertEquals(2, target.size()); // Parameters should remain unchanged since target is immutable
  }

  @Test
  @DisplayName("addBehavior adds WHERE clause when SQL has no WHERE")
  void addBehaviorAddsWhereClauseWhenNoWhere() {
    // Given
    Map<String, String> target = Map.of("category", "electronics", "price", "100.50");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE"));
    assertTrue(result.contains("category='electronics'"));
    assertTrue(result.contains("price=100.50"));
    assertTrue(result.contains(" AND "));
    assertEquals(2, target.size()); // Parameters should remain unchanged since target is immutable
  }

  @Test
  @DisplayName("addBehavior adds AND clause when SQL already has WHERE")
  void addBehaviorAddsAndClauseWhenWhereExists() {
    // Given
    Map<String, String> target = Map.of("category", "electronics", "price", "100.50");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM products WHERE available = true").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE available = true AND"));
    assertTrue(result.contains("category='electronics'"));
    assertTrue(result.contains("price=100.50"));
  }

  @Test
  @DisplayName("addBehavior handles case-insensitive WHERE detection")
  void addBehaviorHandlesCaseInsensitiveWhere() {
    // Given
    Map<String, String> target = Map.of("category", "electronics");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM products where available = true").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products where available = true AND"));
    assertTrue(result.contains("category='electronics'"));
  }

  @Test
  @DisplayName("addBehavior formats numeric values without quotes")
  void addBehaviorFormatsNumericValuesWithoutQuotes() {
    // Given
    Map<String, String> target =
        Map.of("id", "123", "price", "99.99", "quantity", "-5", "rating", "4.5e2");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE"));
    assertTrue(result.contains("id=123"));
    assertTrue(result.contains("price=99.99"));
    assertTrue(result.contains("quantity=-5"));
    assertTrue(result.contains("rating=4.5e2"));
  }

  @Test
  @DisplayName("addBehavior formats string values with quotes")
  void addBehaviorFormatsStringValuesWithQuotes() {
    // Given
    Map<String, String> target =
        Map.of("name", "John Doe", "email", "john@example.com", "status", "active");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM users").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM users WHERE"));
    assertTrue(result.contains("name='John Doe'"));
    assertTrue(result.contains("email='john@example.com'"));
    assertTrue(result.contains("status='active'"));
  }

  @Test
  @DisplayName("addBehavior handles mixed parameter replacement and WHERE addition")
  void addBehaviorHandlesMixedParameterReplacementAndWhereAddition() {
    // Given
    Map<String, String> target =
        Map.of("userId", "123", "category", "electronics", "price", "100.50");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder()
            .sql("SELECT * FROM products WHERE user_id = ${userId}")
            .build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM products WHERE user_id = 123 AND"));
    assertTrue(result.contains("category='electronics'"));
    assertTrue(result.contains("price=100.50"));
  }

  @Test
  @DisplayName("addBehavior handles null SQL gracefully")
  void addBehaviorHandlesNullSql() {
    // Given
    Map<String, String> target = Map.of("category", "electronics");

    DatasourcePayloadDto payload = DatasourcePayloadDto.builder().sql(null).build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertNull(payload.getSql());
    assertEquals(1, target.size()); // Parameters should remain unchanged
  }

  @Test
  @DisplayName("addBehavior handles empty SQL gracefully")
  void addBehaviorHandlesEmptySql() {
    // Given
    Map<String, String> target = Map.of("category", "electronics");

    DatasourcePayloadDto payload = DatasourcePayloadDto.builder().sql("").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("", payload.getSql());
  }

  @Test
  @DisplayName("addBehavior handles null target gracefully")
  void addBehaviorHandlesNullTarget() {
    // Given
    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(null, payload);

    // Then
    assertEquals("SELECT * FROM products", payload.getSql()); // SQL should remain unchanged
  }

  @Test
  @DisplayName("addBehavior handles empty target gracefully")
  void addBehaviorHandlesEmptyTarget() {
    // Given
    Map<String, String> target = Map.of();
    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM products").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    assertEquals("SELECT * FROM products", payload.getSql()); // SQL should remain unchanged
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

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM measurements").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM measurements WHERE"));
    assertTrue(result.contains("intValue=42"));
    assertTrue(result.contains("floatValue=3.14159"));
    assertTrue(result.contains("negativeValue=-123.456"));
    assertTrue(result.contains("scientificValue=1.23e-4"));
    assertTrue(result.contains("scientificValue2=6.022E23"));
  }

  @Test
  @DisplayName("addBehavior handles string values that look like numbers")
  void addBehaviorHandlesStringValuesThatLookLikeNumbers() {
    // Given
    Map<String, String> target =
        Map.of("phone", "123-456-7890", "zipCode", "12345", "partNumber", "ABC123");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM customers").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM customers WHERE"));
    assertTrue(result.contains("phone='123-456-7890'"));
    assertTrue(result.contains("zipCode=12345"));
    assertTrue(result.contains("partNumber='ABC123'"));
  }

  @Test
  @DisplayName("addBehavior processes all parameters")
  void addBehaviorProcessesAllParameters() {
    // Given
    Map<String, String> target = Map.of("z", "last", "a", "first", "m", "middle");

    DatasourcePayloadDto payload =
        DatasourcePayloadDto.builder().sql("SELECT * FROM items").build();

    // When
    decorator.addBehavior(target, payload);

    // Then
    String result = payload.getSql();
    assertTrue(result.startsWith("SELECT * FROM items WHERE"));
    assertTrue(result.contains("a='first'"));
    assertTrue(result.contains("m='middle'"));
    assertTrue(result.contains("z='last'"));
  }
}
