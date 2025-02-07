package org.sitmun.domain.role;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.user.configuration.UserConfiguration;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Role.
 */
@Entity
@Table(name = "STM_ROLE", uniqueConstraints = @UniqueConstraint(name = "STM_ROL_NOM_UK", columnNames = "ROL_NAME"))
@Builder(toBuilder = true)
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
  @JsonView({ClientConfigurationViews.Base.class, ClientConfigurationViews.ApplicationTerritory.class})
  private Integer id;

  /**
   * Role name.
   * Role names are unique and cannot be shared across applications.
   */
  @Column(name = "ROL_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView({ClientConfigurationViews.Base.class, ClientConfigurationViews.ApplicationTerritory.class})
  private String name;

  /**
   * Role description.
   */
  @Column(name = "ROL_NOTE", length = 500)
  private String description;

  /**
   * Applications that have this role granted.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_APP_ROL",
    joinColumns = @JoinColumn(
      name = "ARO_ROLEID", foreignKey = @ForeignKey(name = "STM_ARO_FK_ROL")),
    inverseJoinColumns = @JoinColumn(
      name = "ARO_APPID", foreignKey = @ForeignKey(name = "STM_ARO_FK_APP")))
  @Builder.Default
  @JsonView(ClientConfigurationViews.Base.class)
  private Set<Application> applications = new HashSet<>();

  /**
   * Tasks that have this role.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_ROL_TSK",
    joinColumns = @JoinColumn(
      name = "RTS_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RTS_FK_ROL")),
    inverseJoinColumns = @JoinColumn(
      name = "RTS_TASKID",
      foreignKey = @ForeignKey(name = "STM_RTS_FK_TAS")))
  @Builder.Default
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Set<Task> tasks = new HashSet<>();

  /**
   * Permissions that have this role.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_ROL_GGI",
    joinColumns = @JoinColumn(
      name = "RGG_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RGG_FK_ROL")),
    inverseJoinColumns = @JoinColumn(
      name = "RGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_RGG_FK_GGI")))
  @Builder.Default
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Set<CartographyPermission> permissions = new HashSet<>();

  /**
   * Trees that have this role.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(name = "STM_TREE_ROL",
    joinColumns = @JoinColumn(
      name = "TRO_ROLEID",
      foreignKey = @ForeignKey(name = "STM_TRO_FK_ROL")),
    inverseJoinColumns = @JoinColumn(
      name = "TRO_TREEID",
      foreignKey = @ForeignKey(name = "STM_TRO_FK_TRE")))
  @Builder.Default
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Set<Tree> trees = new HashSet<>();

  /**
   * Users that use this role in a territory.
   */
  @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<UserConfiguration> userConfigurations = new HashSet<>();

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Role)) {
      return false;
    }

    Role other = (Role) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
