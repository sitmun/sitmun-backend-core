package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * User role in a territory.
 */
@Entity
@Table(name = "STM_USR_CONF", uniqueConstraints = {
    @UniqueConstraint(name = "STM_UCF_UK", columnNames = {"UCO_USERID", "UCO_TERID", "UCO_ROLEID",
        "UCO_ROLEMID"})})
public class UserConfiguration {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_USUCONF_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "UCO_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_USUCONF_GEN")
  @Column(name = "UCO_ID", precision = 11)
  private BigInteger id;

  /**
   * User.
   */
  @JoinColumn(name = "UCO_USERID", foreignKey = @ForeignKey(name = "STM_UCF_FK_USU"))
  @NotNull
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  /**
   * Territory.
   */
  @ManyToOne
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "UCO_TERID", foreignKey = @ForeignKey(name = "STM_UCF_FK_TER"))
  private Territory territory;

  /**
   * Role.
   */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  @JoinColumn(name = "UCO_ROLEID", foreignKey = @ForeignKey(name = "STM_UCF_FK_ROL"))
  private Role role;

  /**
   * Role for its children territories in case the user can access the territory through
   * its children.
   */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "UCO_ROLEMID", foreignKey = @ForeignKey(name = "STM_UCF_FK_ROL_M"))
  private Role roleChildren;

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Territory getTerritory() {
    return territory;
  }

  public void setTerritory(Territory territory) {
    this.territory = territory;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Role getRoleChildren() {
    return roleChildren;
  }

  public void setRoleChildren(Role roleChildren) {
    this.roleChildren = roleChildren;
  }
}
