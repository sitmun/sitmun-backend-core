package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

@Entity
@Table(name = "STM_ARBOL")
public class Tree implements Identifiable {

  @TableGenerator(
      name = "STM_ARBOL_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "ARB_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_ARBOL_GEN")
  @Column(name = "ARB_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "ARB_NOMBRE", length = 100)
  private String name;

  @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private Set<TreeNode> nodes = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "STM_ARBROL", joinColumns = @JoinColumn(name = "ARR_CODARB", foreignKey = @ForeignKey(name = "STM_ARR_FK_ARB")), inverseJoinColumns = @JoinColumn(name = "ARR_CORROL", foreignKey = @ForeignKey(name = "STM_ARR_FK_ROL")))
  private Set<Role> availableRoles = new HashSet<>();

  public Set<Role> getAvailableRoles() {
    return availableRoles;
  }

  public void setAvailableRoles(Set<Role> availableRoles) {
    this.availableRoles = availableRoles;
  }

  /**
   * @return the id
   */
  public BigInteger getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<TreeNode> getNodes() {
    return nodes;
  }

  public void setNodes(Set<TreeNode> nodes) {
    this.nodes = nodes;
  }

  public ResourceSupport toResource(RepositoryEntityLinks links) {
    Link selfLink = links.linkForSingleResource(this).withSelfRel();
    ResourceSupport res = new Resource<>(this, selfLink);
    res.add(links.linkForSingleResource(this).slash("availableRoles").withRel("availableRoles"));
    res.add(links.linkForSingleResource(this).slash("nodes").withRel("nodes"));
    return res;
  }

}
