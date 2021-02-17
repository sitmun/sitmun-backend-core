package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Task type.
 */
@Entity
@Table(name = "STM_TSK_TYP")
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

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
