package org.sitmun.domain.application;

import static java.util.Map.*;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import lombok.*;
import org.hibernate.Length;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.application.parameter.ApplicationParameter;
import org.sitmun.domain.application.territory.ApplicationTerritory;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.persistence.type.basic.Http;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;
import org.sitmun.infrastructure.persistence.type.i18n.I18n;
import org.sitmun.infrastructure.persistence.type.list.StringListAttributeConverter;
import org.sitmun.infrastructure.persistence.type.map.HashMapConverter;
import org.sitmun.infrastructure.persistence.type.srs.Srs;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A SITMUN application represents a web-based mapping application.
 *
 * <p>Key features: - Can be configured as private (restricted access) or public - Supports multiple
 * territories and roles - Has configurable parameters and backgrounds - Manages cartography
 * permissions and trees - Tracks creation and modification dates
 *
 * <p>Privacy controls: - Private applications are only accessible to authenticated users - Public
 * applications are available to both authenticated and public users - Role-based access control
 * through availableRoles
 *
 * <p>Territory access: - Can be configured to access parent territories - Can be configured to
 * access children territories - Supports territory-specific configurations
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_APP")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Application {

  /** Application unique identifier. */
  @Id
  @Column(name = "APP_ID")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APP_GEN")
  @TableGenerator(
      name = "STM_APP_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "APP_ID",
      allocationSize = 1)
  @JsonView({
    ClientConfigurationViews.Base.class,
    ClientConfigurationViews.ApplicationTerritory.class
  })
  private Integer id;

  /** Application name. */
  @Column(name = "APP_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @I18n
  private String name;

  /** Application description. */
  @Column(name = "APP_DESCRIPTION", length = PersistenceConstants.LONG_DESCRIPTION)
  @I18n
  private String description;

  /** Link to the application logo. */
  @Column(name = "APP_LOGO", length = PersistenceConstants.URL)
  @Http
  private String logo;

  /** Application type (external or internal). */
  @Column(name = "APP_TYPE", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  @CodeList(CodeListsConstants.APPLICATION_TYPE)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String type;

  /** Title to be shown in the browser and in the application when it is internal. */
  @Column(name = "APP_TITLE", length = PersistenceConstants.SHORT_DESCRIPTION)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @I18n
  private String title;

  /** CSS to use in this application when it is internal. */
  @Column(name = "APP_THEME", length = 30)
  private String theme;

  /** Scales to be used in this application when it is internal. */
  @Column(name = "APP_SCALES", length = 250)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> scales;

  /** Projection to be used in this application when it is internal. */
  @Column(name = "APP_PROJECT", length = PersistenceConstants.IDENTIFIER)
  @Srs
  private String srs;

  /**
   * The JSP viewer to be loaded in this application when it is internal or a link to the external
   * application.
   */
  // TODO: Rename this property to "Url" and change the type to URL.
  @Column(name = "APP_TEMPLATE", length = PersistenceConstants.SHORT_DESCRIPTION)
  private String jspTemplate;

  /**
   * When the application is internal {@code True} if the application refreshes automatically;
   * {@code False} if an "update map" button is required.
   */
  @Column(name = "APP_REFRESH")
  @Builder.Default
  private Boolean treeAutoRefresh = true;

  /** Can access a "parent" territory. */
  @Column(name = "APP_ENTRYS")
  @Builder.Default
  private Boolean accessParentTerritory = false;

  /** Can access a "children" territory. */
  @Column(name = "APP_ENTRYM")
  @Builder.Default
  private Boolean accessChildrenTerritory = false;

  @Column(name = "APP_MAINTENANCE_INFORMATION", length = PersistenceConstants.LONG_DESCRIPTION)
  private String maintenanceInformation;

  @Column(name = "APP_UNAVAILABLE")
  @Builder.Default
  private Boolean isUnavailable = false;

  @Column(name = "APP_LAST_UPDATE")
  @Temporal(TemporalType.TIMESTAMP)
  @LastModifiedDate
  private Date lastUpdate;

  /** Application privacy setting. */
  @Column(name = "APP_PRIVATE")
  @Builder.Default
  @NotNull
  private Boolean appPrivate = false;

  @ManyToOne
  @JoinColumn(name = "APP_CREATORID")
  private User creator;

  /** Situation map when the application is internal. */
  @ManyToOne
  @JoinColumn(name = "APP_GGIID", foreignKey = @ForeignKey(name = "STM_APP_FK_GGI"))
  private CartographyPermission situationMap;

  /** Created date. */
  @Column(name = "APP_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /** Application parameters. */
  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationParameter> parameters = new HashSet<>();

  /** Roles granted in this application. */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
      name = "STM_APP_ROL",
      joinColumns =
          @JoinColumn(name = "ARO_APPID", foreignKey = @ForeignKey(name = "STM_ARO_FK_APP")),
      inverseJoinColumns =
          @JoinColumn(name = "ARO_ROLEID", foreignKey = @ForeignKey(name = "STM_ARO_FK_ROL")))
  @Builder.Default
  private Set<Role> availableRoles = new HashSet<>();

  /** Trees assigned to this application. */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
      name = "STM_APP_TREE",
      joinColumns =
          @JoinColumn(name = "ATR_APPID", foreignKey = @ForeignKey(name = "STM_ATR_FK_APP")),
      inverseJoinColumns =
          @JoinColumn(name = "ATR_TREEID", foreignKey = @ForeignKey(name = "STM_ATR_FK_TRE")))
  @Builder.Default
  private Set<Tree> trees = new HashSet<>();

  /** Backgrounds maps. */
  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationBackground> backgrounds = new HashSet<>();

  /** Territories. */
  @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationTerritory> territories = new HashSet<>();

  /** Derived warnings for application configuration. */
  @Transient
  @JsonView(ClientConfigurationViews.Base.class)
  private List<String> warnings;

  /**
   * Header params for maps sections.
   *
   * @param obj
   */
  @Column(name = "APP_HEADERPARAMS", length = Length.LONG32)
  @Convert(converter = HashMapConverter.class)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Map<String, Object> headerParams =
      of(
          "headerLeftSection", of("logoSitmun", visible()),
          "headerRightSection",
              of(
                  "switchApplication", visible(),
                  "homeMenu", visible(),
                  "switchLanguage", visible(),
                  "profileButton", visible(),
                  "logoutButton", visible()));

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Application other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  static Map<String, Object> visible() {
    return Collections.singletonMap("visible", true);
  }
}
