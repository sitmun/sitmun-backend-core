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

  /** Maximum image width in pixels for format I. */
  @Min(1)
  private Integer maxWidth;

  /** Maximum image height in pixels for format I. */
  @Min(1)
  private Integer maxHeight;

  /** GML geometry kind. Present only on a geometry attribute. */
  private String geometryKind;

  /** True when the administrator chose this attribute as the layer geometry. */
  private Boolean layerGeometry;

  /**
   * Preferred geometry id when {@code geometryKind} is generic Geometry. One of point, multipoint,
   * polyline, polygon, multipolyline, multipolygon.
   */
  private String preferredGeometryType;

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
        && !positive(maxWidth)
        && !positive(maxHeight)
        && (geometryKind == null || geometryKind.isBlank())
        && !Boolean.TRUE.equals(layerGeometry)
        && (preferredGeometryType == null || preferredGeometryType.isBlank())
        && additionalProperties.isEmpty();
  }

  private static boolean positive(Integer value) {
    return value != null && value > 0;
  }
}
