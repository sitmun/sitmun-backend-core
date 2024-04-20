package org.sitmun.domain.cartography.permission;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.role.Role;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Geographic Information Permissions.
 */
@Entity
@Table(name = "STM_GRP_GI")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographyPermission {

  public static final String TYPE_SITUATION_MAP = "M";

  public static final String TYPE_BACKGROUND_MAP = "F";

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_GRP_GI_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "GGI_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GRP_GI_GEN")
  @Column(name = "GGI_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /**
   * Permissions name.
   */
  @Column(name = "GGI_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /**
   * Permissions type.
   */
  @Column(name = "GGI_TYPE", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.CARTOGRAPHY_PERMISSION_TYPE)
  private String type;

  @JsonIgnore
  @Transient
  private String storedType;

  /**
   * The geographic information that the roles can access.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_GGI_GI",
    joinColumns = @JoinColumn(
      name = "GGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_GGG_FK_GGI")),
    inverseJoinColumns = @JoinColumn(
      name = "GGG_GIID",
      foreignKey = @ForeignKey(name = "STM_GGG_FK_GEO")))
  @Builder.Default
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Set<Cartography> members = new HashSet<>();

  /**
   * The the roles allowed to access the members.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_ROL_GGI",
    joinColumns = @JoinColumn(
      name = "RGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_RGG_FK_GGI")),
    inverseJoinColumns = @JoinColumn(
      name = "RGG_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RGG_FK_ROL")))
  @Builder.Default
  private Set<Role> roles = new HashSet<>();

  @OneToMany(mappedBy = "cartographyGroup")
  @Builder.Default
  private Set<Background> backgrounds = new HashSet<>();

  @OneToMany(mappedBy = "situationMap")
  @Builder.Default
  private Set<Application> applications = new HashSet<>();

  @PostLoad
  public void postLoad() {
    storedType = type;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (!(obj instanceof CartographyPermission)) {
        return false;
    }

    CartographyPermission other = (CartographyPermission) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
