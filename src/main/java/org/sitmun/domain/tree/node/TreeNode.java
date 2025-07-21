package org.sitmun.domain.tree.node;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;
import lombok.*;
import org.hibernate.Length;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.tree.Tree;
import org.sitmun.infrastructure.persistence.type.basic.Http;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;
import org.sitmun.infrastructure.persistence.type.i18n.I18n;
import org.sitmun.infrastructure.persistence.type.map.HashMapConverter;

/** Tree node. */
@Entity
@Table(name = "STM_TREE_NOD")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeNode {

  /** Unique identifier. */
  @TableGenerator(
      name = "STM_TREE_NOD_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "TNO_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TREE_NOD_GEN")
  @Column(name = "TNO_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /** Parent node. */
  @JoinColumn(name = "TNO_PARENTID", foreignKey = @ForeignKey(name = "STM_TNO_FK_TNO"))
  @ManyToOne
  private TreeNode parent;

  /** Name. */
  @Column(name = "TNO_NAME", length = 80)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @I18n
  private String name;

  /** Description. */
  @Column(name = "TNO_ABSTRACT", length = PersistenceConstants.SHORT_DESCRIPTION)
  @I18n
  private String description;

  /** Type. */
  @Column(name = "TNO_TYPE", length = PersistenceConstants.IDENTIFIER)
  private String type;

  /** Tooltip text. */
  @Column(name = "TNO_TOOLTIP", length = 100)
  private String tooltip;

  /** Enabled by default. */
  @Column(name = "TNO_ACTIVE")
  private Boolean active;

  /** Radio button type (only if the node is a folder). */
  @Column(name = "TNO_RADIO")
  private Boolean radio;

  /**
   * Specifies the behavior of a folder node in the SITMUN viewer layer tree. False by default, when
   * true all contained layers are loaded when (the user) clicks on the layer title (in the SITMUN
   * viewer).
   */
  @Builder.Default
  @Column(name = "TNO_LOAD_DATA", nullable = false)
  private Boolean loadData = false;

  /** Order of the node within the tree. */
  @Column(name = "TNO_ORDER", precision = 6)
  private Integer order;

  /** URL to metadata. */
  @Column(name = "TNO_METAURL", length = PersistenceConstants.URL)
  @Http
  private String metadataURL;

  /** URL to downloadable (zip) dataset. */
  @Column(name = "TNO_DATAURL", length = PersistenceConstants.URL)
  @Http
  private String datasetURL;

  /** Enable GetMap Filter (if available). */
  @Column(name = "TNO_FILTER_GM")
  private Boolean filterGetMap;

  /** Enable GetFeatureInfo Filter (if available). */
  @Column(name = "TNO_FILTER_GFI")
  private Boolean filterGetFeatureInfo;

  /** Enable GetFeatureInfo (if available). */
  @Column(name = "TNO_QUERYACT")
  private Boolean queryableActive;

  /** Enable Selectable Filter (if available). */
  @Column(name = "TNO_FILTER_SE")
  private Boolean filterSelectable;

  /** Style name. */
  @Column(name = "TNO_STYLE")
  private String style;

  /** Image. */
  @Column(name = "TNO_IMAGE", length = Length.LONG32)
  private String image;

  /** Image name. */
  @Column(name = "TNO_IMAGE_NAME")
  private String imageName;

  /** View mode. */
  @Column(name = "TNO_VIEW_MODE")
  @CodeList(CodeListsConstants.TREE_NODE_VIEWMODE)
  private String viewMode;

  /** Filterable. */
  @Column(name = "TNO_FILTERABLE")
  private Boolean filterable;

  /** Task associated to this node. */
  @JoinColumn(name = "TNO_TASKID", foreignKey = @ForeignKey(name = "STM_TNO_FK_TASK"))
  @ManyToOne
  private Task task;

  /** Tree. */
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "TNO_TREEID", foreignKey = @ForeignKey(name = "STM_TNO_FK_TRE"))
  @ManyToOne
  @NotNull
  private Tree tree;

  /** Cartography associated to this node. */
  @JoinColumn(name = "TNO_GIID", foreignKey = @ForeignKey(name = "STM_TNO_FK_GEO"))
  @ManyToOne
  private Cartography cartography;

  @Column(name = "TNO_MAPPING", length = Length.LONG32)
  @Convert(converter = HashMapConverter.class)
  private Map<String, Object> mapping;

  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  public Integer getParentId() {
    if (parent != null) {
      return parent.id;
    }
    return null;
  }

  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  public Integer getCartographyId() {
    if (cartography != null) {
      return cartography.getId();
    }
    return null;
  }

  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  public Integer getTaskId() {
    if (task != null) {
      return task.getId();
    }
    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TreeNode other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
