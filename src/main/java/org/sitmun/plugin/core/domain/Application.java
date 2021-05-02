package org.sitmun.plugin.core.domain;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.constraints.SpatialReferenceSystem;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.*;

/**
 * A SITMUN application.
 */
@Entity
@Table(name = "STM_APP")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Application {

  /**
   * Application unique identifier.
   */
  @Id
  @Column(name = "APP_ID")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APP_GEN")
  @TableGenerator(name = "STM_APP_GEN", table = "STM_SEQUENCE", pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT", pkColumnValue = "APP_ID", allocationSize = 1)
  @JsonView({Workspace.View.class, WorkspaceApplication.View.class})
  private Integer id;

  /**
   * Application name.
   */
  @Column(name = "APP_NAME", length = IDENTIFIER)
  @NotBlank
  @JsonView({WorkspaceApplication.View.class})
  private String name;

  /**
   * Application type (external or internal).
   */
  @Column(name = "APP_TYPE", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.APPLICATION_TYPE)
  @JsonView({WorkspaceApplication.View.class})
  private String type;

  /**
   * Title to be shown in the browser and in the application when it is internal.
   */
  @Column(name = "APP_TITLE", length = TITLE)
  @JsonView({Workspace.View.class, WorkspaceApplication.View.class})
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
  @JsonView({WorkspaceApplication.View.class})
  private List<String> scales;

  /**
   * Projection to be used in this application when it is internal.
   */
  @Column(name = "APP_PROJECT", length = IDENTIFIER)
  @SpatialReferenceSystem
  @JsonView({WorkspaceApplication.View.class})
  private String srs;

  /**
   * The JSP viewer to be loaded in this application when it is internal or a link to the
   * external application.
   */
  @Column(name = "APP_TEMPLATE", length = SHORT_DESCRIPTION)
  @NotNull
  private String jspTemplate;

  /**
   * When the application is internal {@code True} if the application refreshes automatically;
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
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "APP_GGIID", foreignKey = @ForeignKey(name = "STM_APP_FK_GGI"))
  private CartographyPermission situationMap;

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
  @Builder.Default
  private Set<ApplicationParameter> parameters = new HashSet<>();

  /**
   * Roles granted in this application.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_APP_ROL",
    joinColumns = @JoinColumn(
      name = "ARO_APPID", foreignKey = @ForeignKey(name = "STM_ARO_FK_APP")),
    inverseJoinColumns = @JoinColumn(
      name = "ARO_ROLEID", foreignKey = @ForeignKey(name = "STM_ARO_FK_ROL")))
  @Builder.Default
  private Set<Role> availableRoles = new HashSet<>();

  /**
   * Trees assigned to this application.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_APP_TREE",
    joinColumns = @JoinColumn(
      name = "ATR_APPID", foreignKey = @ForeignKey(name = "STM_ATR_FK_APP")),
    inverseJoinColumns = @JoinColumn(
      name = "ATR_TREEID", foreignKey = @ForeignKey(name = "STM_ATR_FK_TRE")))
  @Builder.Default
  private Set<Tree> trees = new HashSet<>();

  /**
   * Backgrounds maps.
   */
  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationBackground> backgrounds = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Application))
      return false;

    Application other = (Application) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
