package org.sitmun.plugin.core.domain;


import lombok.*;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.constraints.HttpURL;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.*;

/**
 * Territorial entity.
 */
@Entity
@Table(name = "STM_TERRITORY", uniqueConstraints = {
  @UniqueConstraint(name = "STM_TER_NOM_UK", columnNames = {"TER_NAME"})})
@Builder
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
  private String name;

  /**
   * Territorial authority name.
   */
  @Column(name = "TER_ADMNAME", length = SHORT_DESCRIPTION)
  private String territorialAuthorityName;

  /**
   * Territorial authority address.
   */
  @Column(name = "TER_ADDRESS", length = SHORT_DESCRIPTION)
  private String territorialAuthorityAddress;

  /**
   * Territorial authority email.
   */
  @Column(name = "TER_EMAIL", length = IDENTIFIER)
  @Email
  private String territorialAuthorityEmail;

  /**
   * Territory scope.
   */
  @Column(name = "TER_SCOPE", length = IDENTIFIER)
  @CodeList(CodeLists.TERRITORY_SCOPE)
  private String scope;

  /**
   * Link to the territorial authority logo.
   */
  @Column(name = "TER_LOGO", length = URL)
  @HttpURL
  private String territorialAuthorityLogo;

  /**
   * Bounding box of the territory.
   */
  @Column(name = "TER_EXTENT", length = 250)
  private String extent;

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
  private Date createdDate;

  /**
   * Territory typology.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TER_GTYPID", foreignKey = @ForeignKey(name = "STM_TER_FK_TET"))
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
  private Set<UserConfiguration> userConfigurations = new HashSet<>();

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
