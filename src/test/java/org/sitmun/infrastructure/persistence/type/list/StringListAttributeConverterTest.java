package org.sitmun.infrastructure.persistence.type.list;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.AttributeConverter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
  void nullLists() {
    assertNull(converter.convertToDatabaseColumn(null));
  }

  /**
   * Empty lists are converted to empty strings.
   */
  @Test
  void emptyLists() {
    assertEquals("", converter.convertToDatabaseColumn(Collections.emptyList()));
  }

  /**
   * Singletons lists are converted to strings without commas.
   */
  @Test
  void singletonLists() {
    assertEquals("single", converter.convertToDatabaseColumn(Collections.singletonList("single")));
  }

  /**
   * Singletons lists with a <code>null</code> converted to a string with the literal <code>null</code>.
   */
  @Test
  void singletonListOfNull() {
    assertEquals("null", converter.convertToDatabaseColumn(Collections.singletonList(null)));
  }

  /**
   * Lists with more than one element converted to strings where elements are joined with commas.
   */
  @Test
  void listWithElements() {
    assertEquals("one,two", converter.convertToDatabaseColumn(Arrays.asList("one", "two")));
  }

  /**
   * <code>null</code> values in lists are converted to the <code>null</code> literal.
   */
  @Test
  void listWithMulls() {
    assertEquals("one,null,two",
      converter.convertToDatabaseColumn(Arrays.asList("one", null, "two")));
  }

  /**
   * Strings in lists are trimmed before the conversion.
   */
  @Test
  void listWithNullAndWhitespaces() {
    assertEquals("one,null,two",
      converter.convertToDatabaseColumn(Arrays.asList(" one ", null, " two ")));
  }

  /**
   * Null strings are converted to null values.
   */
  @Test
  void nullString() {
    assertNull(converter.convertToEntityAttribute(null));
  }

  /**
   * Blank strings are converted to empty lists.
   */
  @Test
  void emptyString() {
    assertEquals(Collections.emptyList(), converter.convertToEntityAttribute(""));
  }

  /**
   * Blank strings are converted to empty lists.
   */
  @Test
  void blankString() {
    assertTrue(converter.convertToEntityAttribute("   ").isEmpty());
  }

  /**
   * String with commas are split by commas, trimmed and converted to lists.
   */
  @Test
  void stringSeparatedWithCommas() {
    assertEquals(Arrays.asList("one", "two", null, "three"),
      converter.convertToEntityAttribute("one, two,null, three"));
  }

}
