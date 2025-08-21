package org.sitmun.domain.territory.type;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.*;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;

/** Type of territorial entities. */
@Entity
@Table(
    name = "STM_TER_TYP",
    uniqueConstraints = @UniqueConstraint(name = "STM_TET_NOM_UK", columnNames = "TET_NAME"))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerritoryType {

  /** Unique identifier. */
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

  /** Name. */
  @Column(name = "TET_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /** `true` if this is an official type. */
  @Column(name = "TET_OFFICIAL")
  @NotNull
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Boolean official = false;

  /** If {@code true}, the territory is root in the territories' hierarchy. */
  @Column(name = "TET_TOP")
  @NotNull
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Boolean topType = false;

  /** If {@code true}, the territory is leaf in the territories' hierarchy. */
  @Column(name = "TET_BOTTOM")
  @NotNull
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Boolean bottomType = false;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TerritoryType other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
