package org.sitmun.plugin.core.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Geographic Information Permissions.
 */
@Entity
@Table(name = "STM_GRP_GI")
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
  private Integer id;

  /**
   * Permissions name.
   */
  @Column(name = "GGI_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Permissions type.
   */
  @Column(name = "GGI_TYPE", length = IDENTIFIER)
  @CodeList(CodeLists.CARTOGRAPHY_PERMISSION_TYPE)
  private String type;

  @JsonIgnore
  @Transient
  private String storedType;

  /**
   * The geographic information that the roles can access.
   */
  @ManyToMany
  @JoinTable(
    name = "STM_GGI_GI",
    joinColumns = @JoinColumn(
      name = "GGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_GCC_FK_GCA")),
    inverseJoinColumns = @JoinColumn(
      name = "GGG_GIID",
      foreignKey = @ForeignKey(name = "STM_GCC_FK_CAR")))
  private Set<Cartography> members;

  /**
   * The the roles allowed to access the members.
   */
  @ManyToMany
  @JoinTable(
    name = "STM_ROL_GGI",
    joinColumns = @JoinColumn(
      name = "RGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_RGC_FK_GCA")),
    inverseJoinColumns = @JoinColumn(
      name = "RGG_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RGC_FK_ROL")))
  private Set<Role> roles;

  @OneToMany(mappedBy = "cartographyGroup")
  private Set<Background> backgrounds;

  @OneToMany(mappedBy = "situationMap")
  private Set<Application> applications;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Set<Cartography> getMembers() {
    return members;
  }

  public void setMembers(Set<Cartography> members) {
    this.members = members;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public void setRoles(Set<Role> roles) {
    this.roles = roles;
  }

  public CartographyPermission() {
  }

  private CartographyPermission(Integer id, @NotBlank String name, String type, String storedType, Set<Cartography> members, Set<Role> roles, Set<Background> backgrounds, Set<Application> applications) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.storedType = storedType;
    this.members = members;
    this.roles = roles;
    this.backgrounds = backgrounds;
    this.applications = applications;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getStoredType() {
    return storedType;
  }

  public void setStoredType(String storedType) {
    this.storedType = storedType;
  }

  public Set<Background> getBackgrounds() {
    return backgrounds;
  }

  public void setBackgrounds(Set<Background> backgrounds) {
    this.backgrounds = backgrounds;
  }

  public Set<Application> getApplications() {
    return applications;
  }

  public void setApplications(Set<Application> applications) {
    this.applications = applications;
  }

  @PostLoad
  public void postLoad() {
    storedType = type;
  }

  public static class Builder {
    private Integer id;
    private @NotBlank String name;
    private String type;
    private String storedType;
    private Set<Cartography> members;
    private Set<Role> roles;
    private Set<Background> backgrounds;
    private Set<Application> applications;

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setName(@NotBlank String name) {
      this.name = name;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setStoredType(String storedType) {
      this.storedType = storedType;
      return this;
    }

    public Builder setMembers(Set<Cartography> members) {
      this.members = members;
      return this;
    }

    public Builder setRoles(Set<Role> roles) {
      this.roles = roles;
      return this;
    }

    public Builder setBackgrounds(Set<Background> backgrounds) {
      this.backgrounds = backgrounds;
      return this;
    }

    public Builder setApplications(Set<Application> applications) {
      this.applications = applications;
      return this;
    }

    public CartographyPermission build() {
      return new CartographyPermission(id, name, type, storedType, members, roles, backgrounds, applications);
    }
  }
}
