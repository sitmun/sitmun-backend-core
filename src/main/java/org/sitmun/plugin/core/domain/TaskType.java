package org.sitmun.plugin.core.domain;


import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Task type.
 */
@Entity
@Table(name = "STM_TSK_TYP")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskType {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_TSK_TYP_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "TTY_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TSK_TYP_GEN")
  @Column(name = "TTY_ID")
  private Integer id;

  /**
   * Task type name.
   */
  @Column(name = "TTY_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TaskType))
      return false;

    TaskType other = (TaskType) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
