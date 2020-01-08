package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

@Entity
@Table(name = "STM_GRPCARTO")
public class CartographyGroup implements Identifiable {

  @TableGenerator(
      name = "STM_GRPCARTO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "GCA_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GRPCARTO_GEN")
  @Column(name = "GCA_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "GCA_NOMBRE", length = 80)
  private String name;

  @Column(name = "GCA_TIPO", length = 30)
  private String type;

  @ManyToMany
  @JoinTable(name = "STM_GCACAR", joinColumns = @JoinColumn(name = "GCC_CODGCA", foreignKey = @ForeignKey(name = "STM_GCC_FK_GCA")), inverseJoinColumns = @JoinColumn(name = "GCC_CODCAR", foreignKey = @ForeignKey(name = "STM_GCC_FK_CAR")))
  private Set<Cartography> members;

  // roles para los que estará disponible este grupo de cartografía se gestiona
  // desde aquí
  @ManyToMany
  @JoinTable(name = "STM_ROLGCA", joinColumns = @JoinColumn(name = "RGC_CODROL", foreignKey = @ForeignKey(name = "STM_RGC_FK_ROL")), inverseJoinColumns = @JoinColumn(name = "RGC_CODGCA", foreignKey = @ForeignKey(name = "STM_RGC_FK_GCA")))
  private Set<Role> roles;

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

  public ResourceSupport toResource(RepositoryEntityLinks links) {
    Link selfLink = links.linkForSingleResource(this).withSelfRel();
    ResourceSupport res = new Resource<>(this, selfLink);
    res.add(links.linkForSingleResource(this).slash("members").withRel("members"));
    res.add(links.linkForSingleResource(this).slash("roles").withRel("roles"));
    return res;
  }

}
