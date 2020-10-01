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

/**
 * Role.
 */
@Entity
@Table(name = "STM_ROLE", uniqueConstraints = {
    @UniqueConstraint(name = "STM_ROL_NOM_UK", columnNames = {"ROL_NAME"})})
public class Role {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_ROLES_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "ROL_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_ROLES_GEN")
  @Column(name = "ROL_ID", precision = 11)
  private BigInteger id;

  /**
   * Role name.
   * Role names are unique and cannot be shared across applications.
   */
  @NotNull
  @Column(name = "ROL_NAME", nullable = false, length = 250)
  private String name;

  /**
   * Role description.
   */
  @Column(name = "ROL_NOTE", length = 500)
  private String description;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String comments) {
    this.description = comments;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Role) {
      return ((Role) o).getId().equals(this.getId());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
