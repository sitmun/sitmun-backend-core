package org.sitmun.plugin.core.domain;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.validation.constraints.NotBlank;

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
      name = "STM_TIPOTAREA_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "TTY_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TIPOTAREA_GEN")
  @Column(name = "TTY_ID")
  private Integer id;

  /**
   * Task type name.
   */
  @Column(name = "TTY_NAME", length = 30)
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
