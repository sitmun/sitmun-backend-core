package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

@Entity
@Table(name = "STM_CARTO")
public class Cartography implements Identifiable {

  @TableGenerator(
      name = "STM_CARTO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "CAR_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CARTO_GEN")
  @Column(name = "CAR_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "CAR_NOMBRE", length = 100)
  private String name;

  @Column(name = "CAR_TIPO", length = 30)
  private String type;

  @Column(name = "CAR_VISIBLE")
  private Boolean visible;

  @Column(name = "CAR_TRANSP", precision = 11)
  private BigInteger transparency;

  @Column(name = "CAR_QUERYABL")
  private Boolean queryable;

  @Column(name = "CAR_QUERYACT")
  private Boolean queryAct;

  @Column(name = "CAR_QUERYLAY", length = 500)
  private String queryLay;

  @Column(name = "CAR_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @Column(name = "CAR_ORDEN", precision = 11)
  private BigInteger order;

  @Column(name = "CAR_ESC_MIN", precision = 11)
  private BigInteger minimumScale;

  @Column(name = "CAR_ESC_MAX", precision = 11)
  private BigInteger maximumScale;

  // Nombre de las capa
  @Column(name = "CAR_CAPAS", length = 500)
  private String layers;

  @ManyToOne
  @JoinColumn(name = "CAR_CODSER", foreignKey = @ForeignKey(name = "STM_CAR_FK_SER"))
  private Service service;

  @ManyToOne
  @JoinColumn(name = "CAR_CODCON", foreignKey = @ForeignKey(name = "STM_CAR_FK_CON"))
  private Connection connection;

  @OneToMany(mappedBy = "cartography", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CartographyAvailability> availabilities = new HashSet<>();

  // CAR_SELECTABL
  @Column(name = "CAR_SELECTABL")
  private Boolean selectable;

  // CAR_SELECTLAY
  @Column(name = "CAR_SELECTLAY", length = 500)
  private String selectionLayer;

  // CAR_CODSERSEL
  @ManyToOne
  @JoinColumn(name = "CAR_CODSERSEL", foreignKey = @ForeignKey(name = "STM_CAR_FK_SERSEL"))
  private Service selectionService;

  // CAR_LEYENDTIP
  @Column(name = "CAR_LEYENDTIP", length = 50)
  private String legendTip;

  // CAR_LEYENDURL
  @Column(name = "CAR_LEYENDURL", length = 250)
  private String legendUrl;

  // CAR_EDITABLE
  @Column(name = "CAR_EDITABLE")
  private Boolean editable;

  // CAR_METAURL
  @Column(name = "CAR_METAURL")
  private String metadataUrl;

  // CAR_TEMATIZABLE
  @Column(name = "CAR_TEMATIZABLE")
  private Boolean themeable;

  // CAR_TIPOGEOM "POLYGON",...
  @Column(name = "CAR_TIPOGEOM")
  private String geometryType;

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Boolean getVisible() {
    return visible;
  }

  public void setVisible(Boolean visible) {
    this.visible = visible;
  }

  public BigInteger getTransparency() {
    return transparency;
  }

  public void setTransparency(BigInteger transparency) {
    this.transparency = transparency;
  }

  public Boolean getQueryable() {
    return queryable;
  }

  public void setQueryable(Boolean queryable) {
    this.queryable = queryable;
  }

  public Boolean getQueryAct() {
    return queryAct;
  }

  public void setQueryAct(Boolean queryAct) {
    this.queryAct = queryAct;
  }

  public String getQueryLay() {
    return queryLay;
  }

  public void setQueryLay(String queryLay) {
    this.queryLay = queryLay;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
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

  public String getLayers() {
    return layers;
  }

  public void setLayers(String layers) {
    this.layers = layers;
  }

  public Service getService() {
    return service;
  }

  public void setService(Service service) {
    this.service = service;
  }

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public Set<CartographyAvailability> getAvailabilities() {
    return availabilities;
  }

  public void setAvailabilities(Set<CartographyAvailability> availabilities) {
    this.availabilities = availabilities;
  }

  public Boolean getSelectable() {
    return selectable;
  }

  public void setSelectable(Boolean selectable) {
    this.selectable = selectable;
  }

  public String getSelectionLayer() {
    return selectionLayer;
  }

  public void setSelectionLayer(String selectionLayer) {
    this.selectionLayer = selectionLayer;
  }

  public Service getSelectionService() {
    return selectionService;
  }

  public void setSelectionService(Service selectionService) {
    this.selectionService = selectionService;
  }

  public String getLegendTip() {
    return legendTip;
  }

  public void setLegendTip(String legendTip) {
    this.legendTip = legendTip;
  }

  public String getLegendUrl() {
    return legendUrl;
  }

  public void setLegendUrl(String legendUrl) {
    this.legendUrl = legendUrl;
  }

  public Boolean getEditable() {
    return editable;
  }

  public void setEditable(Boolean editable) {
    this.editable = editable;
  }

  public String getMetadataUrl() {
    return metadataUrl;
  }

  public void setMetadataUrl(String metadataUrl) {
    this.metadataUrl = metadataUrl;
  }

  public Boolean getThemeable() {
    return themeable;
  }

  public void setThemeable(Boolean themeable) {
    this.themeable = themeable;
  }

  public String getGeometryType() {
    return geometryType;
  }

  public void setGeometryType(String geometryType) {
    this.geometryType = geometryType;
  }

  public ResourceSupport toResource(RepositoryEntityLinks links) {
    Link selfLink = links.linkForSingleResource(this).withSelfRel();
    ResourceSupport res = new Resource<>(this, selfLink);
    res.add(links.linkForSingleResource(this).slash("availabilities").withRel("availabilities"));
    res.add(links.linkForSingleResource(this).slash("connection").withRel("connection"));
    res.add(links.linkForSingleResource(this).slash("selectionService").withRel("selectionService"));
    res.add(links.linkForSingleResource(this).slash("service").withRel("service"));
    return res;
  }

}
