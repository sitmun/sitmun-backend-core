package org.sitmun.infrastructure.persistence.type.list;

import jakarta.persistence.AttributeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("String List Attribute Converter Test")
class StringListAttributeConverterTest {

  private AttributeConverter<List<String>, String> converter;

  @BeforeEach
  void before() {
    converter = new StringListAttributeConverter();
  }

  /**
   * Null lists are converted to null strings.
   */
  @Test
  @DisplayName("Convert null to database column")
  void convertNullToDatabaseColumn() {
    assertNull(converter.convertToDatabaseColumn(null));
  }

  /**
   * Empty lists are converted to empty strings.
   */
  @Test
  @DisplayName("Convert empty list to database column")
  void convertEmptyListToDatabaseColumn() {
    assertEquals("", converter.convertToDatabaseColumn(Collections.emptyList()));
  }

  /**
   * Singletons lists are converted to strings without commas.
   */
  @Test
  @DisplayName("Convert single value to database column")
  void convertSingleValueToDatabaseColumn() {
    assertEquals("single", converter.convertToDatabaseColumn(Collections.singletonList("single")));
  }

  /**
   * Singletons lists with a <code>null</code> converted to a string with the literal <code>null</code>.
   */
  @Test
  @DisplayName("Convert multiple values to database column")
  void convertMultipleValuesToDatabaseColumn() {
    assertEquals("one,two", converter.convertToDatabaseColumn(Arrays.asList("one", "two")));
  }

  /**
   * Lists with more than one element converted to strings where elements are joined with commas.
   */
  @Test
  @DisplayName("Convert multiple values from database column")
  void convertNullFromDatabaseColumn() {
    assertEquals("one,two",
      converter.convertToDatabaseColumn(Arrays.asList("one", "two")));
  }

  /**
   * <code>null</code> values in lists are converted to the <code>null</code> literal.
   */
  @Test
  @DisplayName("Convert multiple values from database column (with nulls and whitespace)")
  void convertMultipleValuesFromDatabaseColumnWithNullsAndWhitespace() {
    assertEquals("one,null,two",
      converter.convertToDatabaseColumn(Arrays.asList(" one ", null, " two ")));
  }

  /**
   * Empty strings are converted to empty lists.
   */
  @Test
  @DisplayName("Convert empty string from database column")
  void emptyString() {
    assertEquals(Collections.emptyList(), converter.convertToEntityAttribute(""));
  }

  /**
   * Null strings are converted to null values.
   */
  @Test
  @DisplayName("Convert single value from database column")
  void convertSingleValueFromDatabaseColumn() {
    assertNull(converter.convertToEntityAttribute(null));
  }

  /**
   * Blank strings are converted to empty lists.
   */
  @Test
  @DisplayName("Convert blank string from database column")
  void blankString() {
    assertTrue(converter.convertToEntityAttribute("   ").isEmpty());
  }

  /**
   * String with commas are split by commas, trimmed and converted to lists.
   */
  @Test
  @DisplayName("Convert comma-separated string from database column")
  void stringSeparatedWithCommas() {
    assertEquals(Arrays.asList("one", "two", null, "three"),
      converter.convertToEntityAttribute("one, two,null, three"));
  }

}
