package org.sitmun.plugin.core.domain;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Type of territorial entities.
 */
@Entity
@Table(name = "STM_TER_TYP", uniqueConstraints = {
  @UniqueConstraint(name = "STM_TET_NOM_UK", columnNames = {"TET_NAME"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerritoryType {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_TER_TYP_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "TET_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TER_TYP_GEN")
  @Column(name = "TET_ID")
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "TET_NAME", length = IDENTIFIER)
  @NotBlank
  @JsonView({WorkspaceApplication.View.class})
  private String name;

  /**
   * `true` if this is an official type.
   */
  @Column(name = "TET_OFFICIAL")
  @NotBlank
  @JsonView({WorkspaceApplication.View.class})
  private Boolean official = false;

  /**
   * If {@code true}, the territory is root in the territories hierarchy.
   */
  @Column(name = "TET_TOP")
  @NotBlank
  @JsonView({WorkspaceApplication.View.class})
  private Boolean topType = false;

  /**
   * If {@code true}, the territory is leaf in the territories hierarchy.
   */
  @Column(name = "TET_BOTTOM")
  @NotBlank
  @JsonView({WorkspaceApplication.View.class})
  private Boolean bottomType = false;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TerritoryType))
      return false;

    TerritoryType other = (TerritoryType) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
