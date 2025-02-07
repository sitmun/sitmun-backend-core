package org.sitmun.domain.territory.type;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Type of grouping of territorial entities.
 *
 * @deprecated
 */
@Entity
@Table(name = "STM_GTER_TYP", uniqueConstraints = @UniqueConstraint(name = "STM_GTT_NOM_UK", columnNames = "GTT_NAME"))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Deprecated(forRemoval = true)
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
  @Column(name = "GTT_NAME", length = PersistenceConstants.SHORT_DESCRIPTION)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TerritoryGroupType)) {
      return false;
    }

    TerritoryGroupType other = (TerritoryGroupType) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
