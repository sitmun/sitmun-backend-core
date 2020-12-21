package org.sitmun.plugin.core.domain;


import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;


import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.constraints.HttpURL;
import org.sitmun.plugin.core.constraints.SpatialReferenceSystem;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;

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
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "SER_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_SERVICIO_GEN")
  @Column(name = "SER_ID")
  private Integer id;

  /**
   * Service name.
   */
  @Column(name = "SER_NAME", length = IDENTIFIER)
  @NotBlank
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
  @NotNull
  @HttpURL
  private String serviceURL;

  /**
   * List of SRS supported by this service.
   */
  @Column(name = "SER_PROJECTS", length = 1000)
  @Convert(converter = StringListAttributeConverter.class)
  @SpatialReferenceSystem
  private List<String> supportedSRS;

  /**
   * Legend endpoint.
   */
  @Column(name = "SER_LEGEND", length = 250)
  @HttpURL
  private String legendURL;

  /**
   * Get information endpoint.
   */
  @Column(name = "SER_INFOURL", length = 250)
  @HttpURL
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
  @Column(name = "SER_PROTOCOL", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.SERVICE_TYPE)
  private String type;

  /**
   * Native protocol.
   * Required when SITMUN acts as reverse proxy and the origin protocol has a protocol
   * different from the protocol declared in {@link #type}.
   */
  @Column(name = "SER_NAT_PROT", length = IDENTIFIER)
  @CodeList(CodeLists.SERVICE_NATIVE_PROTOCOL)
  private String nativeProtocol;

  /**
   * <code>true</code> if the service is blocked and cannot be used.
   */
  @NotNull
  @Column(name = "SER_BLOCKED")
  private Boolean blocked;

  /**
   * Layers provided by this service.
   */
  @OneToMany(mappedBy = "service", orphanRemoval = true, fetch = FetchType.LAZY)
  private Set<Cartography> layers = new HashSet<>();

  /**
   * Service parameters.
   */
  @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ServiceParameter> parameters = new HashSet<>();

  public Service() {
  }

  private Service(Integer id, @NotBlank String name, String description,
                  @NotNull String serviceURL, List<String> supportedSRS, String legendURL,
                  String getInformationURL, Date createdDate,
                  @NotNull String type, String nativeProtocol,
                  @NotNull Boolean blocked,
                  Set<Cartography> layers,
                  Set<ServiceParameter> parameters) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.serviceURL = serviceURL;
    this.supportedSRS = supportedSRS;
    this.legendURL = legendURL;
    this.getInformationURL = getInformationURL;
    this.createdDate = createdDate;
    this.type = type;
    this.nativeProtocol = nativeProtocol;
    this.blocked = blocked;
    this.layers = layers;
    this.parameters = parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

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

  public List<String> getSupportedSRS() {
    return supportedSRS;
  }

  public void setSupportedSRS(List<String> srs) {
    this.supportedSRS = srs;
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

  public Boolean getBlocked() {
    return blocked;
  }

  public void setBlocked(Boolean blocked) {
    this.blocked = blocked;
  }

  public static class Builder {
    private Integer id;
    private @NotBlank String name;
    private String description;
    private @NotNull String serviceURL;
    private List<String> supportedSRS;
    private String legendURL;
    private String getInformationURL;
    private Date createdDate;
    private @NotNull String type;
    private String nativeProtocol;
    private @NotNull Boolean blocked;
    private Set<Cartography> layers;
    private Set<ServiceParameter> parameters;

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setName(@NotBlank String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setServiceURL(@NotNull String serviceURL) {
      this.serviceURL = serviceURL;
      return this;
    }

    public Builder setSupportedSRS(List<String> supportedSRS) {
      this.supportedSRS = supportedSRS;
      return this;
    }

    public Builder setLegendURL(String legendURL) {
      this.legendURL = legendURL;
      return this;
    }

    public Builder setGetInformationURL(String getInformationURL) {
      this.getInformationURL = getInformationURL;
      return this;
    }

    public Builder setCreatedDate(Date createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Builder setType(@NotNull String type) {
      this.type = type;
      return this;
    }

    public Builder setNativeProtocol(String nativeProtocol) {
      this.nativeProtocol = nativeProtocol;
      return this;
    }

    public Builder setBlocked(@NotNull Boolean blocked) {
      this.blocked = blocked;
      return this;
    }

    public Builder setLayers(Set<Cartography> layers) {
      this.layers = layers;
      return this;
    }

    public Builder setParameters(Set<ServiceParameter> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Service build() {
      return new Service(id, name, description, serviceURL, supportedSRS, legendURL,
          getInformationURL, createdDate, type, nativeProtocol, blocked, layers, parameters);
    }
  }
}
