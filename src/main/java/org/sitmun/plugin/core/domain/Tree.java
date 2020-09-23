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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
//import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
//import org.springframework.hateoas.Identifiable;
//import org.springframework.hateoas.Link;
//import org.springframework.hateoas.Resource;
//import org.springframework.hateoas.ResourceSupport;

/**
 * Tree.
 */
@Entity
@Table(name = "STM_TREE")
public class Tree { // implements Identifiable {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_ARBOL_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "TRE_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_ARBOL_GEN")
  @Column(name = "TRE_ID", precision = 11)
  private BigInteger id;

  /**
   * Tree name.
   */
  @Column(name = "TRE_NAME", length = 100)
  private String name;

  /**
   * Tree owner.
   * If a tree is owned by a user, the owner is the only user authorized to view it.
   */
  @ManyToOne
  @JoinColumn(name = "TRE_USERID")
  private User owner;

  /**
   * All three nodes.
   */
  @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL,
      orphanRemoval = true, fetch = FetchType.EAGER)
  private Set<TreeNode> allNodes = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "STM_TREE_ROL",
      joinColumns = @JoinColumn(
          name = "TRO_TREEID",
          foreignKey = @ForeignKey(name = "STM_ARR_FK_ARB")),
      inverseJoinColumns = @JoinColumn(
          name = "TRO_ROLEID",
          foreignKey = @ForeignKey(name = "STM_ARR_FK_ROL")))
  private Set<Role> availableRoles = new HashSet<>();

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

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public Set<TreeNode> getAllNodes() {
    return allNodes;
  }

  public void setAllNodes(Set<TreeNode> roots) {
    this.allNodes = roots;
  }

  public Set<Role> getAvailableRoles() {
    return availableRoles;
  }

  public void setAvailableRoles(Set<Role> availableRoles) {
    this.availableRoles = availableRoles;
  }

  //  public ResourceSupport toResource(RepositoryEntityLinks links) {
  //    Link selfLink = links.linkForSingleResource(this).withSelfRel();
  //    ResourceSupport res = new Resource<>(this, selfLink);
  //    res.add(links.linkForSingleResource(this).slash("availableRoles")
  //      .withRel("availableRoles"));
  //    res.add(links.linkForSingleResource(this).slash("nodes").withRel("nodes"));
  //    return res;
  //  }

}
