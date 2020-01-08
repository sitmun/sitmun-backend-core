package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "STM_ROLES", uniqueConstraints = {@UniqueConstraint(name = "STM_ROL_NOM_UK", columnNames = {"ROL_NOMBRE"})})
public class Role {

  @TableGenerator(
      name = "STM_ROLES_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "ROL_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_ROLES_GEN")
  @Column(name = "ROL_CODIGO", precision = 11)
  private BigInteger id;

  @NotNull
  @Column(name = "ROL_NOMBRE", nullable = false, length = 250)
  private String name;

  @Column(name = "ROL_OBSERV", length = 500)
  private String comments;

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  @Override
  public boolean equals(Object o) {
    if ((o != null) && (o instanceof Role)) {
      return ((Role) o).getId() == this.getId();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
