package org.sitmun.plugin.core.domain;


import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.validation.constraints.NotBlank;

/**
 * Task group.
 */
@Entity
@Table(name = "STM_GRP_TSK")
public class TaskGroup {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "GTA_CODIGO_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "GTS_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "GTA_CODIGO_GEN")
  @Column(name = "GTS_ID")
  private Integer id;

  /**
   * Task group name.
   */
  @Column(name = "GTS_NAME", length = IDENTIFIER)
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
