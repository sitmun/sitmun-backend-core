package org.sitmun.plugin.core.domain;


import lombok.*;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.constraints.HttpURL;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Geographic information.
 */
@Entity
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
  private Integer id;

  /**
   * Cartography name.
   */
  @Column(name = "GEO_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Cartography description.
   */
  @Column(name = "GEO_ABSTRACT", length = 250)
  private String description;

  /**
   * List of layer identifiers, separated by comas in case of multiple layers.
   */
  @Column(name = "GEO_LAYERS", length = 800)
  @NotNull
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> layers;

  /**
   * Minimum scale visibility.
   */
  @Column(name = "GEO_MINSCALE")
  private Integer minimumScale;

  /**
   * Maximum visibility.
   */
  @Column(name = "GEO_MAXSCALE")
  private Integer maximumScale;

  /**
   * Cartography order appearance.
   */
  @Column(name = "GEO_ORDER")
  private Integer order;

  /**
   * 0 opaque, 100 translucid.
   */
  @Column(name = "GEO_TRANSP")
  @Min(0)
  @Max(100)
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
  @Column(name = "GEO_QUERYLAY", length = 500)
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
  @ManyToOne(fetch = FetchType.LAZY)
  @NotNull
  @JoinColumn(name = "GEO_SERID", foreignKey = @ForeignKey(name = "STM_CAR_FK_SER"))
  private Service service;

  /**
   * If <code>true</code>, the contents of some layers are can be selected.
   */
  @Column(name = "GEO_SELECTABL")
  private Boolean selectableFeatureEnabled;

  /**
   * Layer available for spatial selection.
   */
  @Column(name = "GEO_SELECTLAY", length = 500)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> selectableLayers;

  /**
   * If <code>true</code>, ta filter is applied to spatial selection requests.
   */
  @Column(name = "GEO_FILTER_SE")
  private Boolean applyFilterToSpatialSelection;

  /**
   * Selection service.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "GEO_SERSELID", foreignKey = @ForeignKey(name = "STM_CAR_FK_SERSEL"))
  private Service spatialSelectionService;

  /**
   * Legend type.
   */
  @Column(name = "GEO_LEGENDTIP", length = IDENTIFIER)
  @CodeList(CodeLists.CARTOGRAPHY_LEGEND_TYPE)
  private String legendType;

  /**
   * Legend URL.
   */
  @Column(name = "GEO_LEGENDURL", length = 250)
  @HttpURL
  private String legendURL;

  /**
   * Creation date.
   */
  @Column(name = "GEO_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Connection for spatial selection listbox queries.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "GEO_CONNID", foreignKey = @ForeignKey(name = "STM_CAR_FK_CON"))
  private DatabaseConnection spatialSelectionConnection;

  /**
   * Direct link to a metadata document.
   */
  @Column(name = "GEO_METAURL", length = 250)
  @HttpURL
  private String metadataURL;

  /**
   * Direct link to a dataset file.
   */
  @Column(name = "GEO_DATAURL", length = 4000)
  @HttpURL
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
  @CodeList(CodeLists.CARTOGRAPHY_GEOMETRY_TYPE)
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
  private Set<CartographyAvailability> availabilities;

  /**
   * Styles.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CartographyStyle> styles;

  /**
   * Default style.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "GEO_STYID")
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
  private Set<CartographyFilter> filters;

  /**
   * Parameters.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CartographyParameter> parameters;

  /**
   * Tree nodes.
   */
  @OneToMany(mappedBy = "cartography")
  private Set<TreeNode> treeNodes;

  /**
   * The permissions that this cartography has.
   */
  @ManyToMany
  @JoinTable(
    name = "STM_GGI_GI",
    joinColumns = @JoinColumn(
      name = "GGG_GIID",
      foreignKey = @ForeignKey(name = "STM_GCC_FK_CAR")),
    inverseJoinColumns = @JoinColumn(
      name = "GGG_GGIID",
      foreignKey = @ForeignKey(name = "STM_GCC_FK_GCA")))
  private Set<CartographyPermission> permissions;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Cartography))
      return false;

    Cartography other = (Cartography) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
