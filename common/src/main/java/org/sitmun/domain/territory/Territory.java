package org.sitmun.domain.territory;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.territory.ApplicationTerritory;
import org.sitmun.domain.cartography.availability.CartographyAvailability;
import org.sitmun.domain.task.availability.TaskAvailability;
import org.sitmun.domain.territory.type.TerritoryGroupType;
import org.sitmun.domain.territory.type.TerritoryType;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.infrastructure.persistence.type.basic.Http;
import org.sitmun.infrastructure.persistence.type.boundingbox.BoundingBox;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;
import org.sitmun.infrastructure.persistence.type.envelope.EnvelopeToStringConverter;
import org.sitmun.infrastructure.persistence.type.point.Point;
import org.sitmun.infrastructure.persistence.type.point.PointToStringConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Territorial entity.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_TERRITORY", uniqueConstraints = @UniqueConstraint(name = "STM_TER_NOM_UK", columnNames = "TER_NAME"))
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
  @JsonView({ClientConfigurationViews.Base.class, ClientConfigurationViews.ApplicationTerritory.class})
  private Integer id;

  /**
   * Geographic code.
   */
  @Column(name = "TER_CODTER", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  private String code;

  /**
   * Territory name.
   */
  @Column(name = "TER_NAME", length = PersistenceConstants.SHORT_DESCRIPTION)
  @NotBlank
  @JsonView({ClientConfigurationViews.Base.class, ClientConfigurationViews.ApplicationTerritory.class})
  private String name;

  /**
   * Territorial authority name.
   */
  @Column(name = "TER_ADMNAME", length = PersistenceConstants.SHORT_DESCRIPTION)
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  private String territorialAuthorityName;

  /**
   * Territorial authority address.
   */
  @Column(name = "TER_ADDRESS", length = PersistenceConstants.SHORT_DESCRIPTION)
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  private String territorialAuthorityAddress;

  /**
   * Territorial authority email.
   */
  @Column(name = "TER_EMAIL", length = PersistenceConstants.IDENTIFIER)
  @Email
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  private String territorialAuthorityEmail;

  /**
   * Link to the territorial authority logo.
   */
  @Column(name = "TER_LOGO", length = PersistenceConstants.URL)
  @Http
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  private String territorialAuthorityLogo;

  /**
   * Territory scope.
   *
   * @deprecated
   */
  @Column(name = "TER_SCOPE", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.TERRITORY_SCOPE)
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  @Deprecated(forRemoval = true)
  private String scope;

  /**
   * Bounding box of the territory.
   */
  @Column(name = "TER_EXTENT", length = 250)
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  @Convert(converter = EnvelopeToStringConverter.class)
  @BoundingBox
  private Envelope extent;

  /**
   * Center of the territory.
   *
   * @deprecated Replaced by {@link ApplicationTerritory#getInitialExtent()} because
   * it is not a property of the territory but of the application in this territory,
   * and it is better to use the extent of the view.
   */
  @Column(name = "TER_CENTER", length = PersistenceConstants.VALUE)
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  @Convert(converter = PointToStringConverter.class)
  @Deprecated(forRemoval = true)
  private Point center;

  /**
   * Default zoom level.
   *
   * @deprecated Replaced by {@link ApplicationTerritory#getInitialExtent()} because
   * it is not a property of the territory but of the application in this territory.
   */
  @Column(name = "TER_ZOOM")
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  @Deprecated(forRemoval = true)
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
  @ManyToOne
  @JoinColumn(name = "TER_TYPID", foreignKey = @ForeignKey(name = "STM_TER_FK_TGR"))
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  private TerritoryType type;

  /**
   * Notes.
   */
  @Column(name = "TER_NOTE", length = PersistenceConstants.SHORT_DESCRIPTION)
  private String note;

  /**
   * Legal status of the relationship with the network.
   */
  @Column(name = "TER_LEGAL", length = PersistenceConstants.IDENTIFIER)
  private String legal;

  /**
   * Creation date.
   */
  @Column(name = "TER_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * Territory typology.
   *
   * @deprecated
   */
  @ManyToOne
  @JoinColumn(name = "TER_GTYPID", foreignKey = @ForeignKey(name = "STM_TER_FK_TET"))
  @JsonView({ClientConfigurationViews.ApplicationTerritory.class})
  @Deprecated(forRemoval = true)
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
  @JsonView(ClientConfigurationViews.Base.class)
  private Set<UserConfiguration> userConfigurations = new HashSet<>();

  /**
   * Applications.
   */
  @OneToMany(mappedBy = "territory", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationTerritory> applications = new HashSet<>();

  /**
   * @deprecated Not necessary anymore because the {@link #center} is also deprecated.
   */
  @PostLoad
  @Deprecated(forRemoval = true)
  public void postLoad() {
    if (center == null && extent != null) {
      center = Point.builder()
        .x((extent.getMaxX() - extent.getMinX()) / 2 + extent.getMinX())
        .y((extent.getMaxY() - extent.getMinY()) / 2 + extent.getMinY())
        .build();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (!(obj instanceof Territory)) {
        return false;
    }

    Territory other = (Territory) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
