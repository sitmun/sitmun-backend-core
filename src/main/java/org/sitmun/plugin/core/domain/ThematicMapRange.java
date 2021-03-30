package org.sitmun.plugin.core.domain;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Objects;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;
import static org.sitmun.plugin.core.domain.Constants.SHORT_DESCRIPTION;

/**
 * A range defined in a thematic map.
 */
@Entity
@Table(name = "STM_THE_RANK")
@IdClass(ThematicMapRangeId.class)
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ThematicMapRange {

  /**
   * Thematic map.
   */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "TRK_THEID", foreignKey = @ForeignKey(name = "STM_TRK_FK_THE"))
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
  @Column(name = "TRK_DESC", length = SHORT_DESCRIPTION)
  private String description;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof ThematicMapRange))
      return false;

    ThematicMapRange other = (ThematicMapRange) o;

    return Objects.equals(map, other.map) && Objects.equals(position, other.position);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

