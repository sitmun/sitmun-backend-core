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

@Entity
@Table(name = "STM_SERVICIO")
public class Service {

  @TableGenerator(
      name = "STM_SERVICIO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "SER_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_SERVICIO_GEN")
  @Column(name = "SER_CODIGO", precision = 11)
  private BigInteger id;


  @Column(name = "SER_NOMBRE", length = 30)
  private String name;

  @Column(name = "SER_URL", length = 250)
  private String url;

  @Column(name = "SER_PROJECTS", length = 1000)
  private String projections;

  @Column(name = "SER_LEYENDA", length = 250)
  private String legend;

  @Column(name = "SER_TIPO", length = 30)
  private String type;

  @Column(name = "SER_INFOURL", length = 250)
  private String infoUrl;

  @Column(name = "SER_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @OneToMany(mappedBy = "service", orphanRemoval = true)
  private Set<Cartography> layers = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "SER_CODCON", foreignKey = @ForeignKey(name = "STM_SER_FK_CON"))
  private Connection connection;


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


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getProjections() {
    return projections;
  }

  public void setProjections(String projections) {
    this.projections = projections;
  }

  public String getLegend() {
    return legend;
  }

  public void setLegend(String legend) {
    this.legend = legend;
  }

  public String getInfoUrl() {
    return infoUrl;
  }

  public void setInfoUrl(String infoUrl) {
    this.infoUrl = infoUrl;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Set<Cartography> getLayers() {
    return layers;
  }

  public void setLayers(Set<Cartography> layers) {
    this.layers = layers;
  }

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public Set<ServiceParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Set<ServiceParameter> parameters) {
    this.parameters = parameters;
  }

}
