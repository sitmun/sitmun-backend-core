package org.sitmun.common.domain.territory;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.common.config.CodeLists;
import org.sitmun.common.domain.cartography.availability.CartographyAvailability;
import org.sitmun.common.domain.task.availability.TaskAvailability;
import org.sitmun.common.domain.territory.type.TerritoryGroupType;
import org.sitmun.common.domain.territory.type.TerritoryType;
import org.sitmun.common.domain.user.configuration.UserConfiguration;
import org.sitmun.common.domain.user.position.UserPosition;
import org.sitmun.common.types.boundingbox.BoundingBox;
import org.sitmun.common.types.codelist.CodeList;
import org.sitmun.common.types.envelope.Envelope;
import org.sitmun.common.types.envelope.EnvelopeToStringConverter;
import org.sitmun.common.types.http.Http;
import org.sitmun.common.types.point.Point;
import org.sitmun.common.types.point.PointToStringConverter;
import org.sitmun.feature.client.config.Views;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.sitmun.common.config.PersistenceConstants.*;

/**
 * Territorial entity.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_TERRITORY", uniqueConstraints = {
  @UniqueConstraint(name = "STM_TER_NOM_UK", columnNames = {"TER_NAME"})})
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Territory {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_TERRITORY_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "TER_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TERRITORY_GEN")
  @Column(name = "TER_ID")
  @JsonView({Views.Workspace.class, Views.WorkspaceApplication.class})
  private Integer id;

  /**
   * Geographic code.
   */
  @Column(name = "TER_CODMUN", length = IDENTIFIER)
  @NotBlank
  private String code;

  /**
   * Territory name.
   */
  @Column(name = "TER_NAME", length = SHORT_DESCRIPTION)
  @NotBlank
  @JsonView({Views.Workspace.class, Views.WorkspaceApplication.class})
  private String name;

  /**
   * Territorial authority name.
   */
  @Column(name = "TER_ADMNAME", length = SHORT_DESCRIPTION)
  @JsonView({Views.WorkspaceApplication.class})
  private String territorialAuthorityName;

  /**
   * Territorial authority address.
   */
  @Column(name = "TER_ADDRESS", length = SHORT_DESCRIPTION)
  @JsonView({Views.WorkspaceApplication.class})
  private String territorialAuthorityAddress;

  /**
   * Territorial authority email.
   */
  @Column(name = "TER_EMAIL", length = IDENTIFIER)
  @Email
  @JsonView({Views.WorkspaceApplication.class})
  private String territorialAuthorityEmail;

  /**
   * Link to the territorial authority logo.
   */
  @Column(name = "TER_LOGO", length = URL)
  @Http
  @JsonView({Views.WorkspaceApplication.class})
  private String territorialAuthorityLogo;

  /**
   * Territory scope.
   */
  @Column(name = "TER_SCOPE", length = IDENTIFIER)
  @CodeList(CodeLists.TERRITORY_SCOPE)
  @JsonView({Views.WorkspaceApplication.class})
  @Deprecated
  private String scope;

  /**
   * Bounding box of the territory.
   */
  @Column(name = "TER_EXTENT", length = 250)
  @JsonView({Views.WorkspaceApplication.class})
  @Convert(converter = EnvelopeToStringConverter.class)
  @BoundingBox
  private Envelope extent;

  /**
   * Center of the territory.
   */
  @Column(name = "TER_CENTER", length = VALUE)
  @JsonView({Views.WorkspaceApplication.class})
  @Convert(converter = PointToStringConverter.class)
  private Point center;

  /**
   * Default zoom level.
   */
  @Column(name = "TER_ZOOM")
  @JsonView({Views.WorkspaceApplication.class})
  private Integer defaultZoomLevel;


  /**
   * <code>true</code> if the territory is blocked.
   */
  @Column(name = "TER_BLOCKED")
  @NotNull
  private Boolean blocked;

  /**
   * Territory typology.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TER_TYPID", foreignKey = @ForeignKey(name = "STM_TER_FK_TGR"))
  @JsonView({Views.WorkspaceApplication.class})
  private TerritoryType type;

  /**
   * Notes.
   */
  @Column(name = "TER_NOTE", length = SHORT_DESCRIPTION)
  private String note;

  /**
   * Creation date.
   */
  @Column(name = "TER_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * Territory typology.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TER_GTYPID", foreignKey = @ForeignKey(name = "STM_TER_FK_TET"))
  @JsonView({Views.WorkspaceApplication.class})
  @Deprecated
  private TerritoryGroupType groupType;

  /**
   * Territories that are part of this territory.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_GRP_TER",
    joinColumns = @JoinColumn(
      name = "GTE_TERID",
      foreignKey = @ForeignKey(name = "STM_GTE_FK_TER")),
    inverseJoinColumns = @JoinColumn(
      name = "GTE_TERMID",
      foreignKey = @ForeignKey(name = "STM_GTE_FK_TERM")))
  @Builder.Default
  private Set<Territory> members = new HashSet<>();

  /**
   * Territories of which this territory is part of.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_GRP_TER",
    joinColumns = @JoinColumn(
      name = "GTE_TERMID",
      foreignKey = @ForeignKey(name = "STM_GTE_FK_TERM")),
    inverseJoinColumns = @JoinColumn(
      name = "GTE_TERID",
      foreignKey = @ForeignKey(name = "STM_GTE_FK_TER")))
  @Builder.Default
  private Set<Territory> memberOf = new HashSet<>();

  /**
   * Task availabilities.
   */
  @OneToMany(mappedBy = "territory", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<TaskAvailability> taskAvailabilities = new HashSet<>();

  /**
   * Cartography availabilities.
   */
  @OneToMany(mappedBy = "territory", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<CartographyAvailability> cartographyAvailabilities = new HashSet<>();

  /**
   * Positions available.
   */
  @OneToMany(mappedBy = "territory", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<UserPosition> positions = new HashSet<>();

  /**
   * Users that can access to this territory a role.
   */
  @OneToMany(mappedBy = "territory", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonView(Views.Workspace.class)
  private Set<UserConfiguration> userConfigurations = new HashSet<>();

  @PostLoad
  public void postLoad() {
    if (center == null && extent != null) {
      center = Point.builder()
        .x((extent.getMaxX() - extent.getMinX()) / 2 + extent.getMinX())
        .y((extent.getMaxY() - extent.getMinY()) / 2 + extent.getMinY())
        .build();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Territory))
      return false;

    Territory other = (Territory) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
