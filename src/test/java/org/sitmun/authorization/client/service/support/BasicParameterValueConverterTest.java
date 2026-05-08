package org.sitmun.authorization.client.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

@DisplayName("BasicParameterValueConverter")
class BasicParameterValueConverterTest {

  private final BasicParameterValueConverter converter = new BasicParameterValueConverter();

  @Nested
  @DisplayName("convert(...) with null rawValue")
  class ConvertNullRawValue {

    @Test
    @DisplayName("STRING returns empty string for backward compatibility")
    void stringReturnsEmpty() {
      assertThat(converter.convert(BasicParameterValueType.STRING, null)).isEqualTo("");
    }

    @ParameterizedTest(name = "{0} returns null instead of throwing")
    @EnumSource(
        value = BasicParameterValueType.class,
        mode = Mode.EXCLUDE,
        names = {"STRING"})
    void nonStringReturnsNull(BasicParameterValueType type) {
      assertThat(converter.convert(type, null)).isNull();
    }
  }

  @Nested
  @DisplayName("convert(...) with valid rawValue")
  class ConvertValidRawValue {

    @Test
    void stringReturnsRawValue() {
      assertThat(converter.convert(BasicParameterValueType.STRING, "abc")).isEqualTo("abc");
    }

    @Test
    void numberParsesAsDouble() {
      assertThat(converter.convert(BasicParameterValueType.NUMBER, "1.5")).isEqualTo(1.5d);
    }

    @Test
    void booleanParsesTrueIgnoringCase() {
      assertThat(converter.convert(BasicParameterValueType.BOOLEAN, "TrUe")).isEqualTo(true);
    }

    @Test
    void booleanReturnsFalseForNonTrueString() {
      assertThat(converter.convert(BasicParameterValueType.BOOLEAN, "anything")).isEqualTo(false);
    }

    @Test
    void arrayParsesJsonList() {
      Object result = converter.convert(BasicParameterValueType.ARRAY, "[1,2,3]");
      assertThat(result).asInstanceOf(list(Object.class)).containsExactly(1, 2, 3);
    }

    @Test
    void objectParsesJsonMap() {
      Object result = converter.convert(BasicParameterValueType.OBJECT, "{\"k\":\"v\"}");
      assertThat(result).isInstanceOf(Map.class);
      assertThat(asObjectMap(result)).contains(entry("k", "v"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asObjectMap(Object value) {
      return (Map<String, Object>) value;
    }

    @Test
    void nullTypeAlwaysReturnsNull() {
      assertThat(converter.convert(BasicParameterValueType.NULL, "ignored")).isNull();
    }
  }

  @Nested
  @DisplayName("convert(...) with invalid rawValue")
  class ConvertInvalidRawValue {

    @Test
    void numberReturnsNullOnNumberFormat() {
      assertThat(converter.convert(BasicParameterValueType.NUMBER, "not-a-number")).isNull();
    }

    @Test
    void arrayReturnsNullOnMalformedJson() {
      assertThat(converter.convert(BasicParameterValueType.ARRAY, "[1,2")).isNull();
    }

    @Test
    void objectReturnsNullOnMalformedJson() {
      assertThat(converter.convert(BasicParameterValueType.OBJECT, "{")).isNull();
    }
  }

  @Nested
  @DisplayName("validate(...)")
  class Validate {

    @Test
    void stringAcceptsAnyValueIncludingNull() {
      Errors errors = newErrors();
      converter.validate("p", BasicParameterValueType.STRING, null, errors);
      converter.validate("p", BasicParameterValueType.STRING, 42, errors);
      converter.validate("p", BasicParameterValueType.STRING, "abc", errors);
      assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void numberAcceptsParseableString() {
      Errors errors = newErrors();
      converter.validate("p", BasicParameterValueType.NUMBER, "1.5", errors);
      assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void numberRejectsUnparseableString() {
      Errors errors = newErrors();
      converter.validate("p", BasicParameterValueType.NUMBER, "abc", errors);
      assertThat(errors.hasErrors()).isTrue();
    }

    @Test
    void numberSkipsNonStringInput() {
      Errors errors = newErrors();
      converter.validate("p", BasicParameterValueType.NUMBER, null, errors);
      converter.validate("p", BasicParameterValueType.NUMBER, 1.5d, errors);
      assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void nullRejectsNonNullValue() {
      Errors errors = newErrors();
      converter.validate("p", BasicParameterValueType.NULL, "x", errors);
      assertThat(errors.hasErrors()).isTrue();
    }

    @Test
    void nullAcceptsNullValue() {
      Errors errors = newErrors();
      converter.validate("p", BasicParameterValueType.NULL, null, errors);
      assertThat(errors.hasErrors()).isFalse();
    }

    private Errors newErrors() {
      return new MapBindingResult(new HashMap<>(), "target");
    }
  }
}
