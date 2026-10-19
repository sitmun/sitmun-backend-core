package org.sitmun.domain.cartography.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Feature info format options converter")
class FeatureInfoFormatOptionsConverterTest {

  private FeatureInfoFormatOptionsConverter converter;

  @BeforeEach
  void setUp() {
    converter = new FeatureInfoFormatOptionsConverter();
  }

  @Test
  @DisplayName("empty options are stored as null")
  void emptyOptionsAreNull() {
    FeatureInfoFormatOptions options = new FeatureInfoFormatOptions();
    options.setPadFractionDigits(false);

    assertThat(converter.convertToDatabaseColumn(options)).isNull();
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isNull();
    assertThat(converter.convertToEntityAttribute("  ")).isNull();
  }

  @Test
  @DisplayName("number options round-trip and omit pad when it is off")
  void numberOptionsRoundTrip() {
    FeatureInfoFormatOptions options = new FeatureInfoFormatOptions();
    options.setFractionDigits(0);
    options.setPadFractionDigits(false);

    String json = converter.convertToDatabaseColumn(options);

    assertThat(json).isEqualTo("{\"fractionDigits\":0}");
    FeatureInfoFormatOptions read = converter.convertToEntityAttribute(json);
    assertThat(read.getFractionDigits()).isZero();
    assertThat(read.getPadFractionDigits()).isNull();
  }

  @Test
  @DisplayName("pad and date style round-trip")
  void padAndDateStyleRoundTrip() {
    FeatureInfoFormatOptions padded = new FeatureInfoFormatOptions();
    padded.setFractionDigits(2);
    padded.setPadFractionDigits(true);
    assertThat(converter.convertToDatabaseColumn(padded))
        .isEqualTo("{\"fractionDigits\":2,\"padFractionDigits\":true}");

    FeatureInfoFormatOptions date = new FeatureInfoFormatOptions();
    date.setDateStyle("date");
    assertThat(converter.convertToDatabaseColumn(date)).isEqualTo("{\"dateStyle\":\"date\"}");
  }

  @Test
  @DisplayName("image limits round-trip and a non-positive side is omitted")
  void imageLimitsRoundTrip() {
    FeatureInfoFormatOptions options = new FeatureInfoFormatOptions();
    options.setMaxWidth(240);
    options.setMaxHeight(160);

    assertThat(converter.convertToDatabaseColumn(options))
        .contains("\"maxWidth\":240")
        .contains("\"maxHeight\":160");

    FeatureInfoFormatOptions read =
        converter.convertToEntityAttribute("{\"maxWidth\":240,\"maxHeight\":160}");
    assertThat(read.getMaxWidth()).isEqualTo(240);
    assertThat(read.getMaxHeight()).isEqualTo(160);
    assertThat(read.additionalProperties()).isEmpty();

    FeatureInfoFormatOptions zero = new FeatureInfoFormatOptions();
    zero.setMaxWidth(0);
    assertThat(converter.convertToDatabaseColumn(zero)).isNull();
  }

  @Test
  @DisplayName("unknown keys survive a round-trip")
  void unknownKeysSurvive() {
    FeatureInfoFormatOptions options =
        converter.convertToEntityAttribute("{\"fractionDigits\":1,\"width\":12}");

    assertThat(options.getFractionDigits()).isEqualTo(1);
    assertThat(options.additionalProperties()).containsEntry("width", 12);
    assertThat(converter.convertToDatabaseColumn(options))
        .isEqualTo("{\"fractionDigits\":1,\"width\":12}");
  }
}
