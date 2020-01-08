package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "GTA_CODIGO")
public class TaskGroup {

  @TableGenerator(
      name = "GTA_CODIGO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "GTA_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "GTA_CODIGO_GEN")
  @Column(name = "GTA_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "GTA_NOMBRE", length = 80)
  private String name;

  /**
   * @return the id
   */
  public BigInteger getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
