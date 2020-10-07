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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;
//import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
//import org.springframework.hateoas.Identifiable;
//import org.springframework.hateoas.Link;
//import org.springframework.hateoas.Resource;
//import org.springframework.hateoas.ResourceSupport;

/**
 * An application.
 */
@Entity
@Table(name = "STM_APP")
public class Application { //implements Identifiable {

  /**
   * Application unique identifier.
   */
  @Id
  @Column(name = "APP_ID", precision = 11)
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APPS_GEN")
  @TableGenerator(name = "STM_APPS_GEN", table = "STM_SEQUENCE", pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT", pkColumnValue = "APP_ID", allocationSize = 1)
  private BigInteger id;

  /**
   * Application name.
   */
  @Column(name = "APP_NAME", length = 80)
  private String name;

  /**
   * Application type (external or internal).
   */
  @Column(name = "APP_TYPE", length = 250)
  private String type;

  /**
   * Title to be shown in the browser and in the application.
   */
  @Column(name = "APP_TITLE", length = 250)
  private String title;

  /**
   * CSS to use in this application.
   */
  @Column(name = "APP_THEME", length = 30)
  private String theme;

  /**
   * Scales to be used in this application.
   */
  @Column(name = "APP_SCALES", length = 250)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> scales;

  /**
   * Projection to be used in this application.
   */
  @Column(name = "APP_PROJECT", length = 250)
  private String srs;

  /**
   * The JSP viewer to be loaded in this application.
   * If and Only if the application is a SITMUN 2 application.
   */
  @Column(name = "APP_TEMPLATE")
  @NotNull
  private String jspTemplate;

  /**
   * True if the application refreshes automatically; False if an "update map" button is required.
   */
  @Column(name = "APP_REFRESH")
  private Boolean treeAutoRefresh;

  /**
   * Can access a "parent" territory.
   */
  @Column(name = "APP_ENTRYS")
  private Boolean accessParentTerritory;

  /**
   * Can access a "children" territory.
   */
  @Column(name = "APP_ENTRYM")
  private Boolean accessChildrenTerritory;

  /**
   * Situation map.
   */
  @ManyToOne
  @JoinColumn(name = "APP_GGIID", foreignKey = @ForeignKey(name = "STM_APP_FK_GCA"))
  private CartographyGroup situationMap;

  /**
   * Created date.
   */
  @Column(name = "APP_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Application parameters.
   */
  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ApplicationParameter> parameters = new HashSet<>();

  /**
   * Roles granted in this application.
   */
  @ManyToMany
  @JoinTable(
      name = "STM_APP_ROL",
      joinColumns = @JoinColumn(
          name = "ARO_APPID", foreignKey = @ForeignKey(name = "STM_APR_FK_APP")),
      inverseJoinColumns = @JoinColumn(
          name = "ARO_ROLEID", foreignKey = @ForeignKey(name = "STM_APR_FK_ROL")))
  private Set<Role> availableRoles = new HashSet<>();

  /**
   * Trees assigned to this application.
   */
  @ManyToMany
  @JoinTable(
      name = "STM_APP_TREE",
      joinColumns = @JoinColumn(
          name = "ATR_APPID", foreignKey = @ForeignKey(name = "STM_APA_FK_APP")),
      inverseJoinColumns = @JoinColumn(
          name = "ATR_TREEID", foreignKey = @ForeignKey(name = "STM_APA_FK_ARB")))
  private Set<Tree> trees;

  /**
   * Backgrounds maps.
   */
  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ApplicationBackground> backgrounds = new HashSet<>();

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

  public List<String> getScales() {
    return scales;
  }

  public void setScales(List<String> scales) {
    this.scales = scales;
  }

  public String getSrs() {
    return srs;
  }

  public void setSrs(String srs) {
    this.srs = srs;
  }

  public String getJspTemplate() {
    return jspTemplate;
  }

  public void setJspTemplate(String jspTemplate) {
    this.jspTemplate = jspTemplate;
  }

  public Boolean getTreeAutoRefresh() {
    return treeAutoRefresh;
  }

  public void setTreeAutoRefresh(Boolean treeAutoRefresh) {
    this.treeAutoRefresh = treeAutoRefresh;
  }

  public Boolean getAccessParentTerritory() {
    return accessParentTerritory;
  }

  public void setAccessParentTerritory(Boolean accessParentTerritory) {
    this.accessParentTerritory = accessParentTerritory;
  }

  public Boolean getAccessChildrenTerritory() {
    return accessChildrenTerritory;
  }

  public void setAccessChildrenTerritory(Boolean accessChildrenTerritory) {
    this.accessChildrenTerritory = accessChildrenTerritory;
  }

  public CartographyGroup getSituationMap() {
    return situationMap;
  }

  public void setSituationMap(CartographyGroup situationMap) {
    this.situationMap = situationMap;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date dateCreated) {
    this.createdDate = dateCreated;
  }

  public Set<ApplicationParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Set<ApplicationParameter> parameters) {
    this.parameters = parameters;
  }

  public Set<Role> getAvailableRoles() {
    return availableRoles;
  }

  public void setAvailableRoles(Set<Role> availableRoles) {
    this.availableRoles = availableRoles;
  }

  public Set<Tree> getTrees() {
    return trees;
  }

  public void setTrees(Set<Tree> trees) {
    this.trees = trees;
  }

  public Set<ApplicationBackground> getBackgrounds() {
    return backgrounds;
  }

  public void setBackgrounds(
      Set<ApplicationBackground> backgrounds) {
    this.backgrounds = backgrounds;
  }

  //  public ResourceSupport toResource(RepositoryEntityLinks links) {
  //    Link selfLink = links.linkForSingleResource(this).withSelfRel();
  //    ResourceSupport res = new Resource<>(this, selfLink);
  //    res.add(links.linkForSingleResource(this).slash("availableRoles")
  //    .withRel("availableRoles"));
  //    res.add(links.linkForSingleResource(this).slash("parameters").withRel("parameters"));
  //    res.add(links.linkForSingleResource(this).slash("trees").withRel("trees"));
  //    res.add(links.linkForSingleResource(this).slash("backgrounds").withRel("backgrounds"));
  //    res.add(links.linkForSingleResource(this).slash("situationMap").withRel("situationMap"));
  //    return res;
  //  }
}
