package org.sitmun.domain.user.configuration;


import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.Objects;

/**
 * User role in a territory.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_USR_CONF", uniqueConstraints = @UniqueConstraint(name = "STM_UCF_UK", columnNames = {"UCO_USERID", "UCO_TERID", "UCO_ROLEID",
  "UCO_ROLEM"}))
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
  @JsonView(ClientConfigurationViews.Base.class)
  private Integer id;

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
  @JsonView(ClientConfigurationViews.Base.class)
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

  /**
   * Creation date.
   */
  @Column(name = "UCO_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof UserConfiguration other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
