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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.constraints.SpatialReferenceSystem;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;

/**
 * A SITMUN application.
 */
@Entity
@Table(name = "STM_APP")
public class Application {

  /**
   * Application unique identifier.
   */
  @Id
  @Column(name = "APP_ID")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APPS_GEN")
  @TableGenerator(name = "STM_APPS_GEN", table = "STM_SEQUENCE", pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT", pkColumnValue = "APP_ID", allocationSize = 1)
  private Integer id;

  /**
   * Application name.
   */
  @Column(name = "APP_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Application type (external or internal).
   */
  @Column(name = "APP_TYPE", length = IDENTIFIER)
  @NotNull
  private String type;

  /**
   * Title to be shown in the browser and in the application when it is internal.
   */
  @Column(name = "APP_TITLE", length = 250)
  private String title;

  /**
   * CSS to use in this application when it is internal.
   */
  @Column(name = "APP_THEME", length = 30)
  private String theme;

  /**
   * Scales to be used in this application when it is internal.
   */
  @Column(name = "APP_SCALES", length = 250)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> scales;

  /**
   * Projection to be used in this application when it is internal.
   */
  @Column(name = "APP_PROJECT", length = IDENTIFIER)
  @SpatialReferenceSystem
  private String srs;

  /**
   * The JSP viewer to be loaded in this application when it is internal or a link to the
   * external application.
   */
  @Column(name = "APP_TEMPLATE")
  @NotNull
  private String jspTemplate;

  /**
   * Wheb tge appliction is internal {@code True} if the application refreshes automatically;
   * {@code False} if an "update map" button is required.
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
   * Situation map when the application is internal.
   */
  @ManyToOne
  @JoinColumn(name = "APP_GGIID", foreignKey = @ForeignKey(name = "STM_APP_FK_GCA"))
  private SituationMap situationMap;

  /**
   * Created date.
   */
  @Column(name = "APP_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @NotNull
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

  private Application(Integer id, @NotBlank String name,
                      @NotNull String type, String title, String theme,
                      List<String> scales, String srs,
                      @NotNull String jspTemplate, Boolean treeAutoRefresh,
                      Boolean accessParentTerritory, Boolean accessChildrenTerritory,
                      SituationMap situationMap,
                      @NotNull Date createdDate,
                      Set<ApplicationParameter> parameters,
                      Set<Role> availableRoles,
                      Set<Tree> trees,
                      Set<ApplicationBackground> backgrounds) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.title = title;
    this.theme = theme;
    this.scales = scales;
    this.srs = srs;
    this.jspTemplate = jspTemplate;
    this.treeAutoRefresh = treeAutoRefresh;
    this.accessParentTerritory = accessParentTerritory;
    this.accessChildrenTerritory = accessChildrenTerritory;
    this.situationMap = situationMap;
    this.createdDate = createdDate;
    this.parameters = parameters;
    this.availableRoles = availableRoles;
    this.trees = trees;
    this.backgrounds = backgrounds;
  }

  public Application() {
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

  public SituationMap getSituationMap() {
    return situationMap;
  }

  public void setSituationMap(SituationMap situationMap) {
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

  public static class Builder {
    private Integer id;
    private @NotBlank String name;
    private @NotNull String type;
    private String title;
    private String theme;
    private List<String> scales;
    private String srs;
    private @NotNull String jspTemplate;
    private Boolean treeAutoRefresh;
    private Boolean accessParentTerritory;
    private Boolean accessChildrenTerritory;
    private SituationMap situationMap;
    private @NotNull Date createdDate;
    private Set<ApplicationParameter> parameters;
    private Set<Role> availableRoles;
    private Set<Tree> trees;
    private Set<ApplicationBackground> backgrounds;

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setName(@NotBlank String name) {
      this.name = name;
      return this;
    }

    public Builder setType(@NotNull String type) {
      this.type = type;
      return this;
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder setTheme(String theme) {
      this.theme = theme;
      return this;
    }

    public Builder setScales(List<String> scales) {
      this.scales = scales;
      return this;
    }

    public Builder setSrs(String srs) {
      this.srs = srs;
      return this;
    }

    public Builder setJspTemplate(@NotNull String jspTemplate) {
      this.jspTemplate = jspTemplate;
      return this;
    }

    public Builder setTreeAutoRefresh(Boolean treeAutoRefresh) {
      this.treeAutoRefresh = treeAutoRefresh;
      return this;
    }

    public Builder setAccessParentTerritory(Boolean accessParentTerritory) {
      this.accessParentTerritory = accessParentTerritory;
      return this;
    }

    public Builder setAccessChildrenTerritory(Boolean accessChildrenTerritory) {
      this.accessChildrenTerritory = accessChildrenTerritory;
      return this;
    }

    public Builder setSituationMap(SituationMap situationMap) {
      this.situationMap = situationMap;
      return this;
    }

    public Builder setCreatedDate(@NotNull Date createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Builder setParameters(Set<ApplicationParameter> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder setAvailableRoles(Set<Role> availableRoles) {
      this.availableRoles = availableRoles;
      return this;
    }

    public Builder setTrees(Set<Tree> trees) {
      this.trees = trees;
      return this;
    }

    public Builder setBackgrounds(Set<ApplicationBackground> backgrounds) {
      this.backgrounds = backgrounds;
      return this;
    }

    /**
     * Build the application.
     */
    public Application build() {
      return new Application(id, name, type, title, theme, scales, srs, jspTemplate,
          treeAutoRefresh,
          accessParentTerritory, accessChildrenTerritory, situationMap, createdDate, parameters,
          availableRoles, trees, backgrounds);
    }
  }
}
