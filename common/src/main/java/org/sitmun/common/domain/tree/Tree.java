package org.sitmun.common.domain.tree;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.common.config.PersistenceConstants;
import org.sitmun.common.domain.application.Application;
import org.sitmun.common.domain.role.Role;
import org.sitmun.common.domain.tree.node.TreeNode;
import org.sitmun.common.domain.user.User;
import org.sitmun.common.types.http.Http;
import org.sitmun.common.views.Views;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

/**
 * Tree.
 */
@Entity
@Table(name = "STM_TREE")
@Builder(toBuilder = true)
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
  @JsonView(Views.WorkspaceApplication.class)
  private Integer id;

  /**
   * Tree name.na
   */
  @Column(name = "TRE_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(Views.WorkspaceApplication.class)
  private String name;

  /**
   * Representative image or icon.
   */
  @Column(name = "TRE_IMAGE", length = PersistenceConstants.URL)
  @Http
  @JsonView(Views.WorkspaceApplication.class)
  private String image;

  /**
   * Description.
   */
  @Column(name = "TRE_ABSTRACT", length = PersistenceConstants.SHORT_DESCRIPTION)
  @JsonView(Views.WorkspaceApplication.class)
  private String description;

  /**
   * Tree owner.
   * If a tree is owned by a user, the owner is the only user authorized to view it.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TRE_USERID", foreignKey = @ForeignKey(name = "STM_TRE_FK_USE"))
  private User owner;

  /**
   * All three nodes.
   */
  @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonView(Views.WorkspaceApplication.class)
  private Set<TreeNode> allNodes = new HashSet<>();

  /**
   * Roles that can access to this three.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(name = "STM_TREE_ROL",
    joinColumns = @JoinColumn(
      name = "TRO_TREEID",
      foreignKey = @ForeignKey(name = "STM_TRO_FK_TRE")),
    inverseJoinColumns = @JoinColumn(
      name = "TRO_ROLEID",
      foreignKey = @ForeignKey(name = "STM_TRO_FK_ROL")))
  @Builder.Default
  private Set<Role> availableRoles = new HashSet<>();

  /**
   * Applications that can use this three.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_APP_TREE",
    joinColumns = @JoinColumn(
      name = "ATR_TREEID", foreignKey = @ForeignKey(name = "STM_ATR_FK_TRE")),
    inverseJoinColumns = @JoinColumn(
      name = "ATR_APPID", foreignKey = @ForeignKey(name = "STM_ATR_FK_APP")))
  @Builder.Default
  private Set<Application> availableApplications = new HashSet<>();

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
