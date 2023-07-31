package org.sitmun.infrastructure.persistence.type.point;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PointToStringConverter test")
class PointToStringConverterTest {


  private PointToStringConverter converter;

  @BeforeEach
  void before() {
    converter = new PointToStringConverter();
  }


  /**
   * Null points are converted to null string.
   */
  @Test
  @DisplayName("A null point maps to a null string")
  void nullEnvelope() {
    assertNull(converter.convertToDatabaseColumn(null));
  }

  /**
   * Null strings are converted to null points.
   */
  @Test
  @DisplayName("A null string maps to a null point")
  void nullString() {
    assertNull(converter.convertToEntityAttribute(null));
  }

  /**
   * Points without decimals converted.
   */
  @Test
  @DisplayName("Convert points without decimals")
  void processPointsWithoutDecimals() {
    Point point = Point.builder()
      .x(430492.0)
      .y(4611482.0)
      .build();
    String string = converter.convertToDatabaseColumn(point);
    assertNotNull(string);
    assertEquals("430492 4611482", string);
  }

  /**
   * Points with decimals converted.
   */
  @Test
  @DisplayName("Convert points with decimals")
  void processPointsWithDecimals() {
    Point point = Point.builder()
      .x(430492.3)
      .y(4611482.33)
      .build();
    String string = converter.convertToDatabaseColumn(point);
    assertNotNull(string);
    assertEquals("430492.3 4611482.33", string);
  }

  /**
   * Points with decimals converted.
   */
  @Test
  @DisplayName("Convert envelopes with decimals")
  void processStringsWithDecimals() {
    Point point = converter.convertToEntityAttribute("430492.3 4611482.33");
    assertNotNull(point);
    assertEquals(430492.3, point.getX());
    assertEquals(4611482.33, point.getY());
  }

  /**
   * Points with bad format are ignored.
   */
  @Test
  @DisplayName("Strings bad formed are ignored")
  void stringBadFormedAreIgnored() {
    Point point = converter.convertToEntityAttribute("430492,3 4611482.33");
    assertNull(point);
  }

}
