package org.sitmun.plugin.core.domain;


import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Role.
 */
@Entity
@Table(name = "STM_ROLE", uniqueConstraints = {
  @UniqueConstraint(name = "STM_ROL_NOM_UK", columnNames = {"ROL_NAME"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Role {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_ROLE_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "ROL_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_ROLE_GEN")
  @Column(name = "ROL_ID")
  private Integer id;

  /**
   * Role name.
   * Role names are unique and cannot be shared across applications.
   */
  @Column(name = "ROL_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Role description.
   */
  @Column(name = "ROL_NOTE", length = 500)
  private String description;

  /**
   * Applications that have this role granted.
   */
  @ManyToMany
  @JoinTable(
    name = "STM_APP_ROL",
    joinColumns = @JoinColumn(
      name = "ARO_ROLEID", foreignKey = @ForeignKey(name = "STM_APR_FK_ROL")),
    inverseJoinColumns = @JoinColumn(
      name = "ARO_APPID", foreignKey = @ForeignKey(name = "STM_APR_FK_APP")))
  private Set<Application> applications;

  /**
   * Tasks that have this role.
   */
  @ManyToMany
  @JoinTable(
    name = "STM_ROL_TSK",
    joinColumns = @JoinColumn(
      name = "RTS_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RTA_FK_ROL")),
    inverseJoinColumns = @JoinColumn(
      name = "RTS_TASKID",
      foreignKey = @ForeignKey(name = "STM_RTA_FK_T")))
  private Set<Task> tasks;

  /**
   * Permissions that have this role.
   */
  @ManyToMany
  @JoinTable(
    name = "STM_ROL_GGI",
    joinColumns = @JoinColumn(
      name = "RGG_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RGC_FK_ROL")),
    inverseJoinColumns = @JoinColumn(
      name = "RGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_RGC_FK_GCA")))
  private Set<CartographyPermission> permissions;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Role))
      return false;

    Role other = (Role) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
