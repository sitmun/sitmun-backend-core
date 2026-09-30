package org.sitmun.domain.cartography.parameter;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** Format options stored as JSON in {@code PGI_OPTIONS}. */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureInfoFormatOptions {

  /** Fraction digits for N and P. Empty means 7. */
  @Min(0)
  private Integer fractionDigits;

  /** When true, N and P pad missing fraction digits with zeros. */
  private Boolean padFractionDigits;

  /** Date style for F: date, or datetime. Empty means date and time. */
  private String dateStyle;

  @JsonIgnore
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

  @JsonAnySetter
  public void putAdditionalProperty(String name, Object value) {
    additionalProperties.put(name, value);
  }

  @JsonAnyGetter
  public Map<String, Object> additionalProperties() {
    return additionalProperties;
  }

  @JsonIgnore
  public boolean isEmpty() {
    return fractionDigits == null
        && !Boolean.TRUE.equals(padFractionDigits)
        && (dateStyle == null || dateStyle.isBlank())
        && additionalProperties.isEmpty();
  }
}
