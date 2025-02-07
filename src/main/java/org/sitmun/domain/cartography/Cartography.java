package org.sitmun.domain.cartography;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.cartography.availability.CartographyAvailability;
import org.sitmun.domain.cartography.filter.CartographyFilter;
import org.sitmun.domain.cartography.parameter.CartographyParameter;
import org.sitmun.domain.cartography.parameter.CartographySpatialSelectionParameter;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.style.CartographyStyle;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.infrastructure.persistence.type.basic.Http;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;
import org.sitmun.infrastructure.persistence.type.list.StringListAttributeConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;

import static org.sitmun.domain.PersistenceConstants.*;

/**
 * Geographic information.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_GEOINFO")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Cartography {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_GEOINFO_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "GEO_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GEOINFO_GEN")
  @Column(name = "GEO_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /**
   * Cartography name.
   */
  @Column(name = "GEO_NAME", length = 100)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /**
   * Cartography description.
   */
  @Column(name = "GEO_ABSTRACT", length = LONG_DESCRIPTION)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String description;

  /**
   * List of layer identifiers.
   */
  @Column(name = "GEO_LAYERS", length = 800)
  @NotNull
  @Convert(converter = StringListAttributeConverter.class)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private List<String> layers;

  /**
   * Minimum scale visibility.
   */
  @Column(name = "GEO_MINSCALE")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer minimumScale;

  /**
   * Maximum visibility.
   */
  @Column(name = "GEO_MAXSCALE")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer maximumScale;

  /**
   * Cartography order appearance.
   */
  @Column(name = "GEO_ORDER")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer order;

  /**
   * 0 opaque, 100 translucid.
   */
  @Column(name = "GEO_TRANSP")
  @Min(0)
  @Max(100)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer transparency;

  /**
   * If <code>true</code>, a filter is applied to GetMap requests.
   */
  @Column(name = "GEO_FILTER_GM")
  private Boolean applyFilterToGetMap;

  /**
   * If <code>true</code>, the layers are queryable.
   */
  @Column(name = "GEO_QUERYABL")
  @NotNull
  private Boolean queryableFeatureAvailable;

  /**
   * If <code>true</code>, the queryable feature is enabled.
   * This only applies if the layers are queryable
   */
  @Column(name = "GEO_QUERYACT")
  @NotNull
  private Boolean queryableFeatureEnabled;

  /**
   * List of queryable layers.
   */
  @Column(name = "GEO_QUERYLAY", length = 10 * IDENTIFIER)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> queryableLayers;

  /**
   * If <code>true</code>, a filter is applied to GetFeatureInfo requests.
   */
  @Column(name = "GEO_FILTER_GFI")
  private Boolean applyFilterToGetFeatureInfo;

  /**
   * Type.
   */
  @Column(name = "GEO_TYPE", length = IDENTIFIER)
  private String type;

  /**
   * Portrayal service.
   */
  @ManyToOne
  @NotNull
  @JoinColumn(name = "GEO_SERID", foreignKey = @ForeignKey(name = "STM_GEO_FK_SER"))
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Service service;

  /**
   * If <code>true</code>, the contents of some layers are can be selected.
   *
   * @deprecated Because the spatial selection should be implemented as a {@link Task}.
   */
  @Column(name = "GEO_SELECTABL")
  @Deprecated
  private Boolean selectableFeatureEnabled;

  /**
   * Layer available for spatial selection.
   *
   * @deprecated Because the spatial selection should be implemented as a {@link Task}.
   */
  @Column(name = "GEO_SELECTLAY", length = 10 * IDENTIFIER)
  @Convert(converter = StringListAttributeConverter.class)
  @Deprecated
  private List<String> selectableLayers;

  /**
   * If <code>true</code>, this filter is applied to spatial selection requests.
   *
   * @deprecated Because the spatial selection should be implemented as a {@link Task}.
   */
  @Column(name = "GEO_FILTER_SS")
  @Deprecated
  private Boolean applyFilterToSpatialSelection;

  /**
   * Selection service.
   *
   * @deprecated Because the spatial selection should be implemented as a {@link Task}.
   */
  @ManyToOne
  @JoinColumn(name = "GEO_SERSELID", foreignKey = @ForeignKey(name = "STM_GEO_FK_SERSEL"))
  @Deprecated
  private Service spatialSelectionService;

  /**
   * Legend type.
   */
  @Column(name = "GEO_LEGENDTIP", length = IDENTIFIER)
  @CodeList(CodeListsConstants.CARTOGRAPHY_LEGEND_TYPE)
  private String legendType;

  /**
   * Legend URL.
   */
  @Column(name = "GEO_LEGENDURL", length = URL)
  @Http
  private String legendURL;

  /**
   * Creation date.
   */
  @Column(name = "GEO_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * Connection for spatial selection listbox queries.
   *
   * @deprecated Because the spatial selection should be implemented as a {@link Task}.
   */
  @ManyToOne
  @JoinColumn(name = "GEO_CONNID", foreignKey = @ForeignKey(name = "STM_GEO_FK_CON"))
  @Deprecated
  private DatabaseConnection spatialSelectionConnection;

  /**
   * Direct link to a metadata document in an external application.
   */
  @Column(name = "GEO_METAURL", length = URL)
  @Http
  private String metadataURL;

  /**
   * Direct link to a dataset file in a service.
   */
  @Column(name = "GEO_DATAURL", length = URL)
  @Http
  private String datasetURL;

  /**
   * If <code>true</code>, a thematic map can be created from this layer.
   */
  @Column(name = "GEO_THEMATIC")
  private Boolean thematic;

  /**
   * Geometry type.
   */
  @Column(name = "GEO_GEOMTYPE", length = IDENTIFIER)
  @CodeList(CodeListsConstants.CARTOGRAPHY_GEOMETRY_TYPE)
  private String geometryType;

  /**
   * Grouping source.
   */
  @Column(name = "GEO_SOURCE", length = IDENTIFIER)
  private String source;

  /**
   * Availailability.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true,
    fetch = FetchType.LAZY)
  @Builder.Default
  private Set<CartographyAvailability> availabilities = new HashSet<>();

  /**
   * Styles.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<CartographyStyle> styles = new HashSet<>();

  /**
   * <code>true</code> if the cartography must be treated as a set of cartographies, each defined by a style.
   */
  @NotNull
  @Column(name = "GEO_STYUSEALL")
  @Builder.Default
  private Boolean useAllStyles = false;

  /**
   * Default style.
   *
   * @deprecated
   */
  @OneToOne
  @JoinColumn(name = "GEO_STYID", foreignKey = @ForeignKey(name = "STM_GEO_FK_SGI"))
  @Deprecated
  private CartographyStyle defaultStyle;

  /**
   * <code>true</code> if the cartography is blocked and cannot be used.
   */
  @NotNull
  @Column(name = "GEO_BLOCKED")
  private Boolean blocked;

  /**
   * Filters.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<CartographyFilter> filters = new HashSet<>();

  /**
   * Parameters.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<CartographyParameter> parameters = new HashSet<>();


  /**
   * Spatial selection parameters.
   *
   * @deprecated Because the spatial selection should be implemented as a {@link Task}.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @Deprecated
  private Set<CartographySpatialSelectionParameter> spatialSelectionParameters = new HashSet<>();


  /**
   * Tree nodes.
   */
  @OneToMany(mappedBy = "cartography")
  @Builder.Default
  private Set<TreeNode> treeNodes = new HashSet<>();

  /**
   * The permissions that this cartography has.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_GGI_GI",
    joinColumns = @JoinColumn(
      name = "GGG_GIID",
      foreignKey = @ForeignKey(name = "STM_GGG_FK_GEO")),
    inverseJoinColumns = @JoinColumn(
      name = "GGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_GGG_FK_GGI")))
  @Builder.Default
  private Set<CartographyPermission> permissions = new HashSet<>();

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Cartography)) {
      return false;
    }

    Cartography other = (Cartography) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
