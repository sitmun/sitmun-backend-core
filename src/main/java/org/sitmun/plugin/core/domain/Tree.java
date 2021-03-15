package org.sitmun.plugin.core.domain;


import lombok.*;

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
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
  @Builder.Default
  private Set<TreeNode> allNodes = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "STM_TREE_ROL",
    joinColumns = @JoinColumn(
      name = "TRO_TREEID",
      foreignKey = @ForeignKey(name = "STM_ARR_FK_ARB")),
    inverseJoinColumns = @JoinColumn(
      name = "TRO_ROLEID",
      foreignKey = @ForeignKey(name = "STM_ARR_FK_ROL")))
  private Set<Role> availableRoles;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Tree))
      return false;

    Tree other = (Tree) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
