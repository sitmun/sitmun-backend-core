package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Tree.
 */
@Entity
@Table(name = "STM_TREE")
public class Tree {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_TREE_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "TRE_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TREE_GEN")
  @Column(name = "TRE_ID")
  private Integer id;

  /**
   * Tree name.
   */
  @Column(name = "TRE_NAME", length = IDENTIFIER)
  @NotBlank
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
    orphanRemoval = true, fetch = FetchType.LAZY)
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

}
