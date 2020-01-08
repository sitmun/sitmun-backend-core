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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
@Table(name = "STM_APPS")
public class Application implements Identifiable {

  @TableGenerator(
      name = "STM_APPS_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "APP_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APPS_GEN")
  @Column(name = "APP_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "APP_NOMBRE", length = 80)
  private String name;

  @Column(name = "APP_TIPO", length = 250)
  private String type;

  @Column(name = "APP_TITULO", length = 250)
  private String title;

  @Column(name = "APP_TEMA", length = 30)
  private String theme;

  @Column(name = "APP_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @ManyToMany
  @JoinTable(name = "STM_APPROL", joinColumns = @JoinColumn(name = "APR_CODAPP", foreignKey = @ForeignKey(name = "STM_APR_FK_APP")), inverseJoinColumns = @JoinColumn(name = "APR_CODROL", foreignKey = @ForeignKey(name = "STM_APR_FK_ROL")))
  private Set<Role> availableRoles = new HashSet<>();

  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ApplicationParameter> parameters = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "STM_APPARB", joinColumns = @JoinColumn(name = "APA_CODAPP", foreignKey = @ForeignKey(name = "STM_APA_FK_APP")), inverseJoinColumns = @JoinColumn(name = "APA_CODARB", foreignKey = @ForeignKey(name = "STM_APA_FK_ARB")))
  private Set<Tree> trees;

  // comma-separated values
  @Column(name = "APP_ESCALAS", length = 250)
  private String scales;

  // comma-separated EPSG codes
  @Column(name = "APP_PROJECT", length = 250)
  private String projections;

  @Column(name = "APP_AUTOREFR")
  private Boolean treeAutoRefresh;

  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ApplicationBackground> backgrounds = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "APP_CODGCA", foreignKey = @ForeignKey(name = "STM_APP_FK_GCA"))
  private CartographyGroup situationMap;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTheme() {
    return theme;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Set<Role> getAvailableRoles() {
    return availableRoles;
  }

  public void setAvailableRoles(Set<Role> availableRoles) {
    this.availableRoles = availableRoles;
  }

  public Set<ApplicationParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Set<ApplicationParameter> parameters) {
    this.parameters = parameters;
  }

  public Set<Tree> getTrees() {
    return trees;
  }

  public void setTrees(Set<Tree> trees) {
    this.trees = trees;
  }

  public String getScales() {
    return scales;
  }

  public void setScales(String scales) {
    this.scales = scales;
  }

  public String getProjections() {
    return projections;
  }

  public void setProjections(String projections) {
    this.projections = projections;
  }

  public Boolean getTreeAutoRefresh() {
    return treeAutoRefresh;
  }

  public void setTreeAutoRefresh(Boolean treeAutoRefresh) {
    this.treeAutoRefresh = treeAutoRefresh;
  }

  public Set<ApplicationBackground> getBackgrounds() {
    return backgrounds;
  }

  public void setBackgrounds(Set<ApplicationBackground> backgrounds) {
    this.backgrounds = backgrounds;
  }

  public CartographyGroup getSituationMap() {
    return situationMap;
  }

  public void setSituationMap(CartographyGroup situationMap) {
    this.situationMap = situationMap;
  }

  public ResourceSupport toResource(RepositoryEntityLinks links) {
    Link selfLink = links.linkForSingleResource(this).withSelfRel();
    ResourceSupport res = new Resource<>(this, selfLink);
    res.add(links.linkForSingleResource(this).slash("availableRoles").withRel("availableRoles"));
    res.add(links.linkForSingleResource(this).slash("parameters").withRel("parameters"));
    res.add(links.linkForSingleResource(this).slash("trees").withRel("trees"));
    res.add(links.linkForSingleResource(this).slash("backgrounds").withRel("backgrounds"));
    res.add(links.linkForSingleResource(this).slash("situationMap").withRel("situationMap"));
    return res;
  }

}
