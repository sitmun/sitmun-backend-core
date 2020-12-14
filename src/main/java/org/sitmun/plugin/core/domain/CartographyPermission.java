package org.sitmun.plugin.core.domain;


import java.util.Set;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.validation.constraints.NotBlank;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

/**
 * Geographic Information Permissions.
 */
@Entity
@Table(name = "STM_GRP_GI")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "GGI_TYPE",
    discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("C")
public class CartographyPermission {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_GRPCARTO_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "GGI_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GRPCARTO_GEN")
  @Column(name = "GGI_ID")
  private Integer id;

  /**
   * Permissions name.
   */
  @Column(name = "GGI_NAME", length = 80)
  @NotBlank
  private String name;

  /**
   * Permissions type.
   */
  @Column(name = "GGI_TYPE", length = 30, insertable = false, updatable = false)
  @CodeList(CodeLists.CARTOGRAPHY_PERMISSION_TYPE)
  private String type;

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

}
