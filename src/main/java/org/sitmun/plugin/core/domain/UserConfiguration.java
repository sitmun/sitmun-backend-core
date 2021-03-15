package org.sitmun.plugin.core.domain;


import lombok.*;
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
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof UserConfiguration))
      return false;

    UserConfiguration other = (UserConfiguration) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
