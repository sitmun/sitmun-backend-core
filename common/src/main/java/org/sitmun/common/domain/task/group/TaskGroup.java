package org.sitmun.common.domain.task.group;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.common.config.PersistenceConstants;
import org.sitmun.feature.client.config.Views;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

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
  @JsonView(Views.WorkspaceApplication.class)
  private Integer id;

  /**
   * Task group name.
   */
  @Column(name = "GTS_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(Views.WorkspaceApplication.class)
  private String name;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TaskGroup))
      return false;

    TaskGroup other = (TaskGroup) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
