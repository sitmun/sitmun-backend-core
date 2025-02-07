package org.sitmun.domain.territory.type;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Type of territorial entities.
 */
@Entity
@Table(name = "STM_TER_TYP", uniqueConstraints = @UniqueConstraint(name = "STM_TET_NOM_UK", columnNames = "TET_NAME"))
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
  @Column(name = "TET_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /**
   * `true` if this is an official type.
   */
  @Column(name = "TET_OFFICIAL")
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Boolean official = false;

  /**
   * If {@code true}, the territory is root in the territories' hierarchy.
   */
  @Column(name = "TET_TOP")
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Boolean topType = false;

  /**
   * If {@code true}, the territory is leaf in the territories' hierarchy.
   */
  @Column(name = "TET_BOTTOM")
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Boolean bottomType = false;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TerritoryType)) {
      return false;
    }

    TerritoryType other = (TerritoryType) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
