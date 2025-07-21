package org.sitmun.domain.task.group;


import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;

import java.util.Objects;

/**
 * Task group.
 */
@Entity
@Table(name = "STM_GRP_TSK")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskGroup {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_GRP_TSK_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "GTS_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GRP_TSK_GEN")
  @Column(name = "GTS_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /**
   * Task group name.
   */
  @Column(name = "GTS_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TaskGroup other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
