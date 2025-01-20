package org.sitmun.domain.tree;


import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;
import org.sitmun.infrastructure.utils.ImageTransformer;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Objects;
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
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /**
   * Tree name.
   */
  @Column(name = "TRE_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /**
   * Representative image or icon.
   */
  @Column(name = "TRE_IMAGE")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String image;

  /**
   * Representative image or icon name.
   */
  @Column(name = "TRE_IMAGE_NAME")
  private String imageName;

  /**
   * Description.
   */
  @Column(name = "TRE_ABSTRACT", length = PersistenceConstants.SHORT_DESCRIPTION)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String description;

  /**
   * Type.
   */
  @Column(name = "TRE_TYPE", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.TREE_TYPE)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String type;

  /**
   * Tree owner.
   * If a tree is owned by a user, the owner is the only user authorized to view it.
   */
  @ManyToOne
  @JoinColumn(name = "TRE_USERID", foreignKey = @ForeignKey(name = "STM_TRE_FK_USE"))
  private User owner;

  /**
   * All three nodes.
   */
  @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
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

  @JsonSetter("image")
  public void imageDeserialize(String image) {
	  this.image = ImageTransformer.scaleImage(image, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (!(obj instanceof Tree)) {
        return false;
    }

    Tree other = (Tree) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
