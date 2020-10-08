package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;
//import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
//import org.springframework.hateoas.Identifiable;
//import org.springframework.hateoas.Link;
//import org.springframework.hateoas.Resource;
//import org.springframework.hateoas.ResourceSupport;

/**
 * Geographic information.
 */
@Entity
@Table(name = "STM_GEOINFO")
public class Cartography { //implements Identifiable {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_CARTO_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "GEO_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CARTO_GEN")
  @Column(name = "GEO_ID", precision = 11)
  private BigInteger id;

  /**
   * Cartography name.
   */
  @Column(name = "GEO_NAME", length = 100)
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
  @Column(name = "GEO_MINSCALE", precision = 11)
  private BigInteger minimumScale;

  /**
   * Maximum visibility.
   */
  @Column(name = "GEO_MAXSCALE", precision = 11)
  private BigInteger maximumScale;

  /**
   * Cartography order appearance.
   */
  @Column(name = "GEO_ORDER", precision = 11)
  private BigInteger order;

  /**
   * 0 opaque, 100 translucid.
   */
  @Column(name = "GEO_TRANSP", precision = 11)
  private BigInteger transparency;

  /**
   * If <code>true</code>, a filter is applied to GetMap requests.
   */
  @Column(name = "GEO_FILTER_GM")
  @NotNull
  private Boolean applyFilterToGetMap;

  /**
   * If <code>true</code>, the layers are queryable.
   */
  @Column(name = "GEO_QUERYABL")
  private Boolean queryableFeatureAvailable;

  /**
   * If <code>true</code>, the queryable feature is enabled.
   * This only applies if the layers are queryable
   */
  @Column(name = "GEO_QUERYACT")
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
  @NotNull
  private Boolean applyFilterToGetFeatureInfo;

  /**
   * Type.
   */
  @Column(name = "GEO_TYPE", length = 30)
  private String type;

  /**
   * Portrayal service.
   */
  @ManyToOne
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
  @NotNull
  private Boolean applyFilterToSpatialSelection;

  /**
   * Selection service.
   */
  @ManyToOne
  @JoinColumn(name = "GEO_SERSELID", foreignKey = @ForeignKey(name = "STM_CAR_FK_SERSEL"))
  private Service spatialSelectionService;

  /**
   * Legend type.
   */
  @Column(name = "GEO_LEGENDTIP", length = 500)
  @CodeList(CodeLists.legendType)
  private String legendType;

  /**
   * Legend URL.
   */
  @Column(name = "GEO_LEGENDURL", length = 250)
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
  @ManyToOne
  @JoinColumn(name = "GEO_CONNID", foreignKey = @ForeignKey(name = "STM_CAR_FK_CON"))
  private Connection spatialSelectionConnection;

  /**
   * Direct link to a metadata document.
   */
  @Column(name = "GEO_METAURL", length = 250)
  private String metadataURL;

  /**
   * Direct link to a dataset file.
   */
  @Column(name = "GEO_DATAURL", length = 4000)
  private String datasetURL;

  /**
   * If <code>true</code>, a thematic map can be created from this layer.
   */
  @Column(name = "GEO_THEMATIC")
  private Boolean thematic;

  /**
   * Geometry type.
   */
  @Column(name = "GEO_GEOMTYPE", length = 50)
  private String geometryType;

  /**
   * Grouping source.
   */
  @Column(name = "GEO_SOURCE", length = 80)
  private String source;

  /**
   * Availailability.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CartographyAvailability> availabilities = new HashSet<>();

  /**
   * Styles.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CartographyStyle> styles = new HashSet<>();

  /**
   * Default style.
   */
  @ManyToOne
  @JoinColumn(name = "GEO_STYID")
  private CartographyStyle defaultStyle;

  /**
   * Filters.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CartographyFilter> filters = new HashSet<>();

  /**
   * Parameters.
   */
  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CartographyParameter> parameters = new HashSet<>();

  public Cartography() {
  }

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getLayers() {
    return layers;
  }

  public void setLayers(List<String> layers) {
    this.layers = layers;
  }

  public BigInteger getMinimumScale() {
    return minimumScale;
  }

  public void setMinimumScale(BigInteger minimumScale) {
    this.minimumScale = minimumScale;
  }

  public BigInteger getMaximumScale() {
    return maximumScale;
  }

  public void setMaximumScale(BigInteger maximumScale) {
    this.maximumScale = maximumScale;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
  }

  public BigInteger getTransparency() {
    return transparency;
  }

  public void setTransparency(BigInteger transparency) {
    this.transparency = transparency;
  }

  public Boolean getApplyFilterToGetMap() {
    return applyFilterToGetMap;
  }

  public void setApplyFilterToGetMap(Boolean applyFilterToGetMap) {
    this.applyFilterToGetMap = applyFilterToGetMap;
  }

  public Boolean getQueryableFeatureAvailable() {
    return queryableFeatureAvailable;
  }

  public void setQueryableFeatureAvailable(Boolean queryableFeatureAvailable) {
    this.queryableFeatureAvailable = queryableFeatureAvailable;
  }

  public Boolean getQueryableFeatureEnabled() {
    return queryableFeatureEnabled;
  }

  public void setQueryableFeatureEnabled(Boolean queryableFeatureEnabled) {
    this.queryableFeatureEnabled = queryableFeatureEnabled;
  }

  public List<String> getQueryableLayers() {
    return queryableLayers;
  }

  public void setQueryableLayers(List<String> queryableLayers) {
    this.queryableLayers = queryableLayers;
  }

  public Boolean getApplyFilterToGetFeatureInfo() {
    return applyFilterToGetFeatureInfo;
  }

  public void setApplyFilterToGetFeatureInfo(Boolean applyFilterToGetFeatureInfo) {
    this.applyFilterToGetFeatureInfo = applyFilterToGetFeatureInfo;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Service getService() {
    return service;
  }

  public void setService(Service portrayalService) {
    this.service = portrayalService;
  }

  public Boolean getSelectableFeatureEnabled() {
    return selectableFeatureEnabled;
  }

  public void setSelectableFeatureEnabled(Boolean selectableFeatureEnabled) {
    this.selectableFeatureEnabled = selectableFeatureEnabled;
  }

  public List<String> getSelectableLayers() {
    return selectableLayers;
  }

  public void setSelectableLayers(List<String> selectableLayers) {
    this.selectableLayers = selectableLayers;
  }

  public Boolean getApplyFilterToSpatialSelection() {
    return applyFilterToSpatialSelection;
  }

  public void setApplyFilterToSpatialSelection(Boolean applyFilterToSpatialSelection) {
    this.applyFilterToSpatialSelection = applyFilterToSpatialSelection;
  }

  public Service getSpatialSelectionService() {
    return spatialSelectionService;
  }

  public void setSpatialSelectionService(Service spatialSelectionService) {
    this.spatialSelectionService = spatialSelectionService;
  }

  public String getLegendType() {
    return legendType;
  }

  public void setLegendType(String legendType) {
    this.legendType = legendType;
  }

  public String getLegendURL() {
    return legendURL;
  }

  public void setLegendURL(String legendURL) {
    this.legendURL = legendURL;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Connection getSpatialSelectionConnection() {
    return spatialSelectionConnection;
  }

  public void setSpatialSelectionConnection(
      Connection spatialSelectionConnection) {
    this.spatialSelectionConnection = spatialSelectionConnection;
  }

  public String getMetadataURL() {
    return metadataURL;
  }

  public void setMetadataURL(String metadataURL) {
    this.metadataURL = metadataURL;
  }

  public String getDatasetURL() {
    return datasetURL;
  }

  public void setDatasetURL(String datasetURL) {
    this.datasetURL = datasetURL;
  }

  public Boolean getThematic() {
    return thematic;
  }

  public void setThematic(Boolean thematic) {
    this.thematic = thematic;
  }

  public String getGeometryType() {
    return geometryType;
  }

  public void setGeometryType(String geometryType) {
    this.geometryType = geometryType;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Set<CartographyAvailability> getAvailabilities() {
    return availabilities;
  }

  public void setAvailabilities(
      Set<CartographyAvailability> availabilities) {
    this.availabilities = availabilities;
  }

  public Set<CartographyStyle> getStyles() {
    return styles;
  }

  public void setStyles(Set<CartographyStyle> styles) {
    this.styles = styles;
  }

  public CartographyStyle getDefaultStyle() {
    return defaultStyle;
  }

  public void setDefaultStyle(CartographyStyle defaultStyle) {
    this.defaultStyle = defaultStyle;
  }

  public Set<CartographyFilter> getFilters() {
    return filters;
  }

  public void setFilters(Set<CartographyFilter> filters) {
    this.filters = filters;
  }

  public Set<CartographyParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Set<CartographyParameter> parameters) {
    this.parameters = parameters;
  }

  private Cartography(BigInteger id, String name, String description,
                      @NotNull List<String> layers, BigInteger minimumScale,
                      BigInteger maximumScale, BigInteger order, BigInteger transparency,
                      @NotNull Boolean applyFilterToGetMap,
                      Boolean queryableFeatureAvailable, Boolean queryableFeatureEnabled,
                      List<String> queryableLayers,
                      @NotNull Boolean applyFilterToGetFeatureInfo, String type,
                      @NotNull Service service, Boolean selectableFeatureEnabled,
                      List<String> selectableLayers,
                      @NotNull Boolean applyFilterToSpatialSelection,
                      Service spatialSelectionService, String legendType, String legendURL,
                      Date createdDate,
                      Connection spatialSelectionConnection, String metadataURL,
                      String datasetURL, Boolean thematic, String geometryType,
                      String source,
                      Set<CartographyAvailability> availabilities,
                      Set<CartographyStyle> styles,
                      CartographyStyle defaultStyle,
                      Set<CartographyFilter> filters,
                      Set<CartographyParameter> parameters) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.layers = layers;
    this.minimumScale = minimumScale;
    this.maximumScale = maximumScale;
    this.order = order;
    this.transparency = transparency;
    this.applyFilterToGetMap = applyFilterToGetMap;
    this.queryableFeatureAvailable = queryableFeatureAvailable;
    this.queryableFeatureEnabled = queryableFeatureEnabled;
    this.queryableLayers = queryableLayers;
    this.applyFilterToGetFeatureInfo = applyFilterToGetFeatureInfo;
    this.type = type;
    this.service = service;
    this.selectableFeatureEnabled = selectableFeatureEnabled;
    this.selectableLayers = selectableLayers;
    this.applyFilterToSpatialSelection = applyFilterToSpatialSelection;
    this.spatialSelectionService = spatialSelectionService;
    this.legendType = legendType;
    this.legendURL = legendURL;
    this.createdDate = createdDate;
    this.spatialSelectionConnection = spatialSelectionConnection;
    this.metadataURL = metadataURL;
    this.datasetURL = datasetURL;
    this.thematic = thematic;
    this.geometryType = geometryType;
    this.source = source;
    this.availabilities = availabilities;
    this.styles = styles;
    this.defaultStyle = defaultStyle;
    this.filters = filters;
    this.parameters = parameters;
  }

  //  public ResourceSupport toResource(RepositoryEntityLinks links) {
  //    Link selfLink = links.linkForSingleResource(this).withSelfRel();
  //    ResourceSupport res = new Resource<>(this, selfLink);
  //    res.add(links.linkForSingleResource(this).slash("availabilities")
  //      .withRel("availabilities"));
  //    res.add(links.linkForSingleResource(this).slash("connection").withRel("connection"));
  //    res.add(links.linkForSingleResource(this).slash("selectionService")
  //      .withRel("selectionService"));
  //    res.add(links.linkForSingleResource(this).slash("service").withRel("service"));
  //    return res;
  //  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private BigInteger id;
    private String name;
    private String description;
    private @NotNull List<String> layers;
    private BigInteger minimumScale;
    private BigInteger maximumScale;
    private BigInteger order;
    private BigInteger transparency;
    private @NotNull Boolean applyFilterToGetMap;
    private Boolean queryableFeatureAvailable;
    private Boolean queryableFeatureEnabled;
    private List<String> queryableLayers;
    private @NotNull Boolean applyFilterToGetFeatureInfo;
    private String type;
    private @NotNull Service service;
    private Boolean selectableFeatureEnabled;
    private List<String> selectableLayers;
    private @NotNull Boolean applyFilterToSpatialSelection;
    private Service spatialSelectionService;
    private String legendType;
    private String legendURL;
    private Date createdDate;
    private Connection spatialSelectionConnection;
    private String metadataURL;
    private String datasetURL;
    private Boolean thematic;
    private String geometryType;
    private String source;
    private Set<CartographyAvailability> availabilities;
    private Set<CartographyStyle> styles;
    private CartographyStyle defaultStyle;
    private Set<CartographyFilter> filters;
    private Set<CartographyParameter> parameters;

    public Builder setId(BigInteger id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setLayers(@NotNull List<String> layers) {
      this.layers = layers;
      return this;
    }

    public Builder setMinimumScale(BigInteger minimumScale) {
      this.minimumScale = minimumScale;
      return this;
    }

    public Builder setMaximumScale(BigInteger maximumScale) {
      this.maximumScale = maximumScale;
      return this;
    }

    public Builder setOrder(BigInteger order) {
      this.order = order;
      return this;
    }

    public Builder setTransparency(BigInteger transparency) {
      this.transparency = transparency;
      return this;
    }

    public Builder setApplyFilterToGetMap(@NotNull Boolean applyFilterToGetMap) {
      this.applyFilterToGetMap = applyFilterToGetMap;
      return this;
    }

    public Builder setQueryableFeatureAvailable(Boolean queryableFeatureAvailable) {
      this.queryableFeatureAvailable = queryableFeatureAvailable;
      return this;
    }

    public Builder setQueryableFeatureEnabled(Boolean queryableFeatureEnabled) {
      this.queryableFeatureEnabled = queryableFeatureEnabled;
      return this;
    }

    public Builder setQueryableLayers(List<String> queryableLayers) {
      this.queryableLayers = queryableLayers;
      return this;
    }

    public Builder setApplyFilterToGetFeatureInfo(
        @NotNull Boolean applyFilterToGetFeatureInfo) {
      this.applyFilterToGetFeatureInfo = applyFilterToGetFeatureInfo;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setService(@NotNull Service service) {
      this.service = service;
      return this;
    }

    public Builder setSelectableFeatureEnabled(Boolean selectableFeatureEnabled) {
      this.selectableFeatureEnabled = selectableFeatureEnabled;
      return this;
    }

    public Builder setSelectableLayers(List<String> selectableLayers) {
      this.selectableLayers = selectableLayers;
      return this;
    }

    public Builder setApplyFilterToSpatialSelection(
        @NotNull Boolean applyFilterToSpatialSelection) {
      this.applyFilterToSpatialSelection = applyFilterToSpatialSelection;
      return this;
    }

    public Builder setSpatialSelectionService(Service spatialSelectionService) {
      this.spatialSelectionService = spatialSelectionService;
      return this;
    }

    public Builder setLegendType(String legendType) {
      this.legendType = legendType;
      return this;
    }

    public Builder setLegendURL(String legendURL) {
      this.legendURL = legendURL;
      return this;
    }

    public Builder setCreatedDate(Date createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Builder setSpatialSelectionConnection(Connection spatialSelectionConnection) {
      this.spatialSelectionConnection = spatialSelectionConnection;
      return this;
    }

    public Builder setMetadataURL(String metadataURL) {
      this.metadataURL = metadataURL;
      return this;
    }

    public Builder setDatasetURL(String datasetURL) {
      this.datasetURL = datasetURL;
      return this;
    }

    public Builder setThematic(Boolean thematic) {
      this.thematic = thematic;
      return this;
    }

    public Builder setGeometryType(String geometryType) {
      this.geometryType = geometryType;
      return this;
    }

    public Builder setSource(String source) {
      this.source = source;
      return this;
    }

    public Builder setAvailabilities(Set<CartographyAvailability> availabilities) {
      this.availabilities = availabilities;
      return this;
    }

    public Builder setStyles(Set<CartographyStyle> styles) {
      this.styles = styles;
      return this;
    }

    public Builder setDefaultStyle(CartographyStyle defaultStyle) {
      this.defaultStyle = defaultStyle;
      return this;
    }

    public Builder setFilters(Set<CartographyFilter> filters) {
      this.filters = filters;
      return this;
    }

    public Builder setParameters(Set<CartographyParameter> parameters) {
      this.parameters = parameters;
      return this;
    }

    /**
     * Cartography builder.
     * @return a new Cartography object.
     */
    public Cartography build() {
      return new Cartography(id, name, description, layers, minimumScale, maximumScale, order,
          transparency, applyFilterToGetMap, queryableFeatureAvailable, queryableFeatureEnabled,
          queryableLayers, applyFilterToGetFeatureInfo, type, service, selectableFeatureEnabled,
          selectableLayers, applyFilterToSpatialSelection, spatialSelectionService, legendType,
          legendURL, createdDate, spatialSelectionConnection, metadataURL, datasetURL, thematic,
          geometryType, source, availabilities, styles, defaultStyle, filters, parameters);
    }
  }
}
