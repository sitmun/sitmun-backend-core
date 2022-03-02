package org.sitmun.common.types.envelope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EnvelopeToStringConverter test")
public class EnvelopeToStringConverterTest {

  private EnvelopeToStringConverter converter;

  @BeforeEach
  public void before() {
    converter = new EnvelopeToStringConverter();
  }

  /**
   * Null envelopes are converted to null string.
   */
  @Test
  @DisplayName("A null envelope maps to a null string")
  public void nullEnvelope() {
    assertNull(converter.convertToDatabaseColumn(null));
  }

  /**
   * Null strings are converted to null envelopes.
   */
  @Test
  @DisplayName("A null string maps to a null envelope")
  public void nullString() {
    assertNull(converter.convertToEntityAttribute(null));
  }

  /**
   * Legacy strings are converted to envelopes.
   */
  @Test
  @DisplayName("Process legacy strings")
  public void processLegacyString() {
    Envelope envelope = converter.convertToEntityAttribute("430492 4611482 437423 4618759");
    assertNotNull(envelope);
    assertEquals(430492, envelope.getMinX());
    assertEquals(4611482, envelope.getMinY());
    assertEquals(437423, envelope.getMaxX());
    assertEquals(4618759, envelope.getMaxY());
  }

  /**
   * Envelopes without decimals converted.
   */
  @Test
  @DisplayName("Convert envelopes without decimals")
  public void processEnvelopesWithoutDecimals() {
    Envelope envelope = Envelope.builder()
      .minX(430492.0)
      .minY(4611482.0)
      .maxX(437423.0)
      .maxY(4618759.0)
      .build();
    String string = converter.convertToDatabaseColumn(envelope);
    assertNotNull(string);
    assertEquals("430492 4611482 437423 4618759", string);
  }

  /**
   * Envelopes with decimals converted.
   */
  @Test
  @DisplayName("Convert envelopes with decimals")
  public void processEnvelopesWithDecimals() {
    Envelope envelope = Envelope.builder()
      .minX(430492.3)
      .minY(4611482.33)
      .maxX(437423.333)
      .maxY(4618759.3333)
      .build();
    String string = converter.convertToDatabaseColumn(envelope);
    assertNotNull(string);
    assertEquals("430492.3 4611482.33 437423.333 4618759.3333", string);
  }

  /**
   * Envelopes with decimals converted.
   */
  @Test
  @DisplayName("Convert envelopes with decimals")
  public void processStringsWithDecimals() {
    Envelope envelope = converter.convertToEntityAttribute("430492.3 4611482.33 437423.333 4618759.3333");
    assertNotNull(envelope);
    assertEquals(430492.3, envelope.getMinX());
    assertEquals(4611482.33, envelope.getMinY());
    assertEquals(437423.333, envelope.getMaxX());
    assertEquals(4618759.3333, envelope.getMaxY());
  }

  /**
   * Envelopes with decimals converted.
   */
  @Test
  @DisplayName("Strings bad formed are ignored")
  public void stringBadFormedAreIgnored() {
    Envelope envelope = converter.convertToEntityAttribute("430492,3 4611482.33 437423.333 4618759.3333");
    assertNull(envelope);
  }
}
