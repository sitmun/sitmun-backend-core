package org.sitmun.plugin.core.domain;


import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.SHORT_DESCRIPTION;

/**
 * Type of grouping of territorial entities.
 */
@Entity
@Table(name = "STM_GTER_TYP", uniqueConstraints = {
  @UniqueConstraint(name = "STM_GTT_NOM_UK", columnNames = {"GTT_NAME"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerritoryGroupType {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_GTER_TYP_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "GTT_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GTER_TYP_GEN")
  @Column(name = "GTT_ID")
  private Integer id;

  /**
   * Name.
   */
  @NotBlank
  @Column(name = "GTT_NAME", length = SHORT_DESCRIPTION)
  private String name;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TerritoryGroupType))
      return false;

    TerritoryGroupType other = (TerritoryGroupType) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
