package org.sitmun.plugin.core.domain;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;


import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

/**
 * A range defined in a thematic map.
 */
@Entity
@Table(name = "STM_THE_RANK")
@IdClass(ThematicMapRangeId.class)
public class ThematicMapRange {

  /**
   * Thematic map.
   */
  @ManyToOne
  @JoinColumn(name = "TRK_THEID")
  @Id
  private ThematicMap map;

  /**
   * Range position.
   */
  @Id
  @Column(name = "TRK_POSITION")
  @Min(0)
  private Integer position;

  /**
   * Range name.
   */
  @Column(name = "TRK_NAME", length = IDENTIFIER)
  private String name;

  /**
   * Allow null values in the range.
   */
  @Column(name = "TRK_VALUENUL")
  private Boolean allowNullValues;

  /**
   * The value that defines the range if the range creation method is <i>unique value</i>
   * and the value is of text type.
   */
  @Column(name = "TRK_VALUE", length = 30)
  private String value;

  /**
   * The minimum range value.
   */
  @Column(name = "TRK_VALUEMIN", scale = 11, precision = 19)
  private BigDecimal minimumValue;

  /**
   * The maximum range value.
   */
  @Column(name = "TRK_VALUEMAX", scale = 11, precision = 19)
  private BigDecimal maximumValue;

  /**
   * Range fill style.
   */
  @Column(name = "TRK_STYLEINT", length = 30)
  @CodeList(CodeLists.THEMATIC_MAP_RANGE_STYLE)
  private String fillStyle;

  /**
   * Range fill color.
   */
  @Column(name = "TRK_COLORINT", length = 30)
  private String fillColor;

  /**
   * Range border style.
   */
  @Column(name = "TRK_STYLE", length = 30)
  @CodeList(CodeLists.THEMATIC_MAP_RANGE_STYLE)
  private String borderStyle;

  /**
   * Range border color.
   */
  @Column(name = "TRK_COLOR", length = 30)
  private String borderColor;

  /**
   * Range border size (width).
   */
  @Column(name = "TRK_SIZE")
  private Integer borderSize;

  /**
   * Range description.
   */
  @Column(name = "TRK_DESC", length = 250)
  private String description;

  public ThematicMap getMap() {
    return map;
  }

  public void setMap(ThematicMap map) {
    this.map = map;
  }

  public Integer getPosition() {
    return position;
  }

  public void setPosition(Integer position) {
    this.position = position;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getAllowNullValues() {
    return allowNullValues;
  }

  public void setAllowNullValues(Boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public BigDecimal getMinimumValue() {
    return minimumValue;
  }

  public void setMinimumValue(BigDecimal minimumValue) {
    this.minimumValue = minimumValue;
  }

  public BigDecimal getMaximumValue() {
    return maximumValue;
  }

  public void setMaximumValue(BigDecimal maximumValue) {
    this.maximumValue = maximumValue;
  }

  public String getFillStyle() {
    return fillStyle;
  }

  public void setFillStyle(String fillStyle) {
    this.fillStyle = fillStyle;
  }

  public String getFillColor() {
    return fillColor;
  }

  public void setFillColor(String fillColor) {
    this.fillColor = fillColor;
  }

  public String getBorderStyle() {
    return borderStyle;
  }

  public void setBorderStyle(String borderStyle) {
    this.borderStyle = borderStyle;
  }

  public String getBorderColor() {
    return borderColor;
  }

  public void setBorderColor(String borderColor) {
    this.borderColor = borderColor;
  }

  public Integer getBorderSize() {
    return borderSize;
  }

  public void setBorderSize(Integer borderSize) {
    this.borderSize = borderSize;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

