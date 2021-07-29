package org.sitmun.plugin.core.domain;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;
import static org.sitmun.plugin.core.domain.Constants.VALUE;

/**
 * Geographic Information parameter for spatial selection.
 */
@Entity
@Table(name = "STM_PAR_SGI")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographySpatialSelectionParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_PAR_SGI_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "PSG_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PAR_SGI_GEN")
  @Column(name = "PSG_ID")
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "PSG_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Value.
   */
  @Column(name = "PSG_VALUE", length = VALUE)
  @NotNull
  private String value;

  /**
   * Format.
   */
  @Column(name = "PSG_FORMAT", length = IDENTIFIER)
  @CodeList(CodeLists.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_FORMAT)
  private String format;

  /**
   * Type.
   */
  @Column(name = "PSG_TYPE", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE)
  private String type;

  /**
   * Cartography that owns this parameter.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PSG_GIID", foreignKey = @ForeignKey(name = "STM_PGI_FK_GEO"))
  @NotNull
  private Cartography cartography;

  /**
   * Order.
   */
  @Column(name = "PSG_ORDER")
  @Min(0)
  private Integer order;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof CartographySpatialSelectionParameter))
      return false;

    CartographySpatialSelectionParameter other = (CartographySpatialSelectionParameter) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
