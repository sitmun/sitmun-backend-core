package org.sitmun.plugin.core.domain;


import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * User role in a territory.
 */
@Entity
@Table(name = "STM_USR_CONF", uniqueConstraints = {
  @UniqueConstraint(name = "STM_UCF_UK", columnNames = {"UCO_USERID", "UCO_TERID", "UCO_ROLEID",
    "UCO_ROLEM"})})
public class UserConfiguration {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_USR_CONF_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "UCO_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_USR_CONF_GEN")
  @Column(name = "UCO_ID")
  private Integer id;

  /**
   * User.
   */
  @JoinColumn(name = "UCO_USERID", foreignKey = @ForeignKey(name = "STM_UCF_FK_USU"))
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  /**
   * Territory.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "UCO_TERID", foreignKey = @ForeignKey(name = "STM_UCF_FK_TER"))
  private Territory territory;

  /**
   * Role.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  @JoinColumn(name = "UCO_ROLEID", foreignKey = @ForeignKey(name = "STM_UCF_FK_ROL"))
  private Role role;

  /**
   * Role applies to children territories in case the user can access the territory through
   * its children.
   * <p>
   * This role only applies when {@link Application#getAccessChildrenTerritory()} is {@code true}.
   */
  @Column(name = "UCO_ROLEM")
  @NotNull
  private Boolean appliesToChildrenTerritories;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
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

  public UserConfiguration() {
  }

  private UserConfiguration(Integer id, @NotNull User user, @NotNull Territory territory, @NotNull Role role, @NotNull Boolean appliesToChildrenTerritories) {
    this.id = id;
    this.user = user;
    this.territory = territory;
    this.role = role;
    this.appliesToChildrenTerritories = appliesToChildrenTerritories;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Boolean getAppliesToChildrenTerritories() {
    return appliesToChildrenTerritories;
  }

  public void setAppliesToChildrenTerritories(Boolean appliesToChildrenTerritories) {
    this.appliesToChildrenTerritories = appliesToChildrenTerritories;
  }

  public static class Builder {
    private Integer id;
    private @NotNull User user;
    private @NotNull Territory territory;
    private @NotNull Role role;
    private @NotNull Boolean appliesToChildrenTerritories;

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setUser(@NotNull User user) {
      this.user = user;
      return this;
    }

    public Builder setTerritory(@NotNull Territory territory) {
      this.territory = territory;
      return this;
    }

    public Builder setRole(@NotNull Role role) {
      this.role = role;
      return this;
    }

    public Builder setAppliesToChildrenTerritories(@NotNull Boolean appliesToChildrenTerritories) {
      this.appliesToChildrenTerritories = appliesToChildrenTerritories;
      return this;
    }

    public UserConfiguration build() {
      return new UserConfiguration(id, user, territory, role, appliesToChildrenTerritories);
    }
  }
}
