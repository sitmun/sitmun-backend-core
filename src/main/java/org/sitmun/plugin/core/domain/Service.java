package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Service.
 */
@Entity
@Table(name = "STM_SERVICE")
public class Service {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_SERVICIO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "SER_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_SERVICIO_GEN")
  @Column(name = "SER_ID", precision = 11)
  private BigInteger id;

  /**
   * Service name.
   */
  @Column(name = "SER_NAME", length = 30)
  private String name;

  /**
   * Service description.
   */
  @Column(name = "SER_ABSTRACT", length = 250)
  private String description;

  /**
   * Service endpoint.
   */
  @Column(name = "SER_URL", length = 250)
  private String serviceURL;

  /**
   * List of SRS supported by this service.
   */
  @Column(name = "SER_PROJECTS", length = 1000)
  private String srs;

  /**
   * Legend endpoint.
   */
  @Column(name = "SER_LEGEND", length = 250)
  private String legendURL;

  /**
   * Get information endpoint.
   */
  @Column(name = "SER_INFOURL", length = 250)
  private String getInformationURL;

  /**
   * Created date.
   */
  @Column(name = "SER_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Service type.
   * The protocol that can be used to request the service.
   */
  @Column(name = "SER_PROTOCOL", length = 30)
  private String type;

  /**
   * Native protocol.
   * Required when SITMUN acts as reverse proxy and the origin protocol has a protocol
   * different from the protocol declared in {@link #type}.
   */
  @Column(name = "SER_NAT_PROTOCOL", length = 30)
  private String nativeProtocol;

  /**
   * Layers provided by this service.
   */
  @OneToMany(mappedBy = "service", orphanRemoval = true)
  private Set<Cartography> layers = new HashSet<>();

  /**
   * Service parameters.
   */
  @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ServiceParameter> parameters = new HashSet<>();

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

  public String getServiceURL() {
    return serviceURL;
  }

  public void setServiceURL(String serviceURL) {
    this.serviceURL = serviceURL;
  }

  public String getSrs() {
    return srs;
  }

  public void setSrs(String srs) {
    this.srs = srs;
  }

  public String getLegendURL() {
    return legendURL;
  }

  public void setLegendURL(String legendURL) {
    this.legendURL = legendURL;
  }

  public String getGetInformationURL() {
    return getInformationURL;
  }

  public void setGetInformationURL(String getInformationURL) {
    this.getInformationURL = getInformationURL;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getNativeProtocol() {
    return nativeProtocol;
  }

  public void setNativeProtocol(String nativeProtocol) {
    this.nativeProtocol = nativeProtocol;
  }

  public Set<Cartography> getLayers() {
    return layers;
  }

  public void setLayers(Set<Cartography> layers) {
    this.layers = layers;
  }

  public Set<ServiceParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Set<ServiceParameter> parameters) {
    this.parameters = parameters;
  }
}
