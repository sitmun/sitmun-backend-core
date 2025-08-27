package org.sitmun.infrastructure.persistence.type.map;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HashMap Converter Test")
class HashMapConverterTest {

  private HashMapConverter converter;

  @BeforeEach
  void setUp() {
    converter = new HashMapConverter();
  }

  @Test
  @DisplayName("Should handle all data types correctly")
  void shouldHandleAllDataTypesCorrectly() {
    // Given
    Map<String, Object> testMap = new HashMap<>();
    testMap.put("stringValue", "test string");
    testMap.put("intValue", 42);
    testMap.put("longValue", 1234567890000L);
    testMap.put("doubleValue", 3.14159);
    testMap.put("floatValue", 2.718f);
    testMap.put("booleanValue", true);
    testMap.put("nullValue", null);

    // When
    String jsonString = converter.convertToDatabaseColumn(testMap);
    Map<String, Object> convertedBack = converter.convertToEntityAttribute(jsonString);

    // Then
    assertThat(convertedBack).containsEntry("stringValue", "test string")
      .containsEntry("intValue", 42)
      .containsEntry("longValue", 1234567890000L)
      .containsEntry("doubleValue", 3.14159)
      .containsEntry("floatValue", 2.718)
      .containsEntry("booleanValue", true)
      .containsEntry("nullValue", null);
  }

  @Test
  @DisplayName("Should handle nested maps correctly")
  void shouldHandleNestedMapsCorrectly() {
    // Given
    Map<String, Object> nestedMap = new HashMap<>();
    nestedMap.put("level1Key", "level1Value");
    
    Map<String, Object> level2Map = new HashMap<>();
    level2Map.put("level2Key", "level2Value");
    nestedMap.put("level2", level2Map);
    
    Map<String, Object> level3Map = new HashMap<>();
    level3Map.put("level3Key", "level3Value");
    level2Map.put("level3", level3Map);

    // When
    String jsonString = converter.convertToDatabaseColumn(nestedMap);
    Map<String, Object> convertedBack = converter.convertToEntityAttribute(jsonString);

    // Then
    assertThat(convertedBack).containsEntry("level1Key", "level1Value").containsKey("level2");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> retrievedLevel2 = (Map<String, Object>) convertedBack.get("level2");
    assertThat(retrievedLevel2).containsEntry("level2Key", "level2Value").containsKey("level3");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> retrievedLevel3 = (Map<String, Object>) retrievedLevel2.get("level3");
    assertThat(retrievedLevel3).containsEntry("level3Key", "level3Value");
  }

  @Test
  @DisplayName("Should handle null input and output correctly")
  void shouldHandleNullInputAndOutputCorrectly() {
    // When & Then
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
  }

  @Test
  @DisplayName("Should handle empty and blank strings correctly")
  void shouldHandleEmptyAndBlankStringsCorrectly() {
    // When & Then
    assertThat(converter.convertToEntityAttribute("")).isNull();
    assertThat(converter.convertToEntityAttribute("   ")).isNull();
    assertThat(converter.convertToEntityAttribute("\t\n")).isNull();
  }

  @Test
  @DisplayName("Should handle empty map correctly")
  void shouldHandleEmptyMapCorrectly() {
    // Given
    Map<String, Object> emptyMap = new HashMap<>();

    // When
    String jsonString = converter.convertToDatabaseColumn(emptyMap);
    Map<String, Object> convertedBack = converter.convertToEntityAttribute(jsonString);

    // Then
    assertThat(jsonString).isNotNull().isEqualTo("{}");
    assertThat(convertedBack).isNotNull().isEmpty();
  }

  @Test
  @DisplayName("Should preserve array values correctly")
  void shouldPreserveArrayValuesCorrectly() {
    // Given
    Map<String, Object> testMap = new HashMap<>();
    testMap.put("arrayValue", new Object[]{1, "two", 3.0, true, null});

    // When
    String jsonString = converter.convertToDatabaseColumn(testMap);
    Map<String, Object> convertedBack = converter.convertToEntityAttribute(jsonString);

    // Then
    assertThat(convertedBack).containsKey("arrayValue");
    assertThat(convertedBack.get("arrayValue")).isInstanceOf(ArrayList.class);
    
    @SuppressWarnings("unchecked")
    Object[] retrievedArray = ((ArrayList<Object>) convertedBack.get("arrayValue")).toArray();
    assertThat(retrievedArray).hasSize(5);
    assertThat(retrievedArray[0]).isEqualTo(1);
    assertThat(retrievedArray[1]).isEqualTo("two");
    assertThat(retrievedArray[2]).isEqualTo(3.0);
    assertThat(retrievedArray[3]).isEqualTo(true);
    assertThat(retrievedArray[4]).isNull();
  }

  @Test
  @DisplayName("Should handle special characters in strings")
  void shouldHandleSpecialCharactersInStrings() {
    // Given
    Map<String, Object> testMap = new HashMap<>();
    testMap.put("specialChars", "test with \"quotes\", 'apostrophes', \n newlines, \t tabs, and unicode: ñáéíóú");

    // When
    String jsonString = converter.convertToDatabaseColumn(testMap);
    Map<String, Object> convertedBack = converter.convertToEntityAttribute(jsonString);

    // Then
    assertThat(convertedBack).containsEntry("specialChars", "test with \"quotes\", 'apostrophes', \n newlines, \t tabs, and unicode: ñáéíóú");
  }

  @Test
  @DisplayName("Should handle very large nested structures")
  void shouldHandleVeryLargeNestedStructures() {
    // Given
    Map<String, Object> largeMap = new HashMap<>();
    Map<String, Object> currentLevel = largeMap;
    
    // Create a deeply nested structure
    for (int i = 0; i < 10; i++) {
      Map<String, Object> nextLevel = new HashMap<>();
      nextLevel.put("level" + i, "value" + i);
      currentLevel.put("nested" + i, nextLevel);
      currentLevel = nextLevel;
    }

    // When
    String jsonString = converter.convertToDatabaseColumn(largeMap);
    Map<String, Object> convertedBack = converter.convertToEntityAttribute(jsonString);

    // Then
    assertThat(convertedBack).isNotNull();
    assertThat(jsonString).isNotNull().isNotEmpty();
    
    // Verify the structure is preserved
    Map<String, Object> currentRetrieved = convertedBack;
    for (int i = 0; i < 10; i++) {
      assertThat(currentRetrieved).containsKey("nested" + i);
      @SuppressWarnings("unchecked")
      Map<String, Object> nextLevel = (Map<String, Object>) currentRetrieved.get("nested" + i);
      assertThat(nextLevel).containsEntry("level" + i, "value" + i);
      currentRetrieved = nextLevel;
    }
  }
}

