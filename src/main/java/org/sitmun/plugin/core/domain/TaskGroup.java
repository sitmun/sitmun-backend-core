package org.sitmun.plugin.core.domain;


import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

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
  private Integer id;

  /**
   * Task group name.
   */
  @Column(name = "GTS_NAME", length = IDENTIFIER)
  @NotBlank
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
