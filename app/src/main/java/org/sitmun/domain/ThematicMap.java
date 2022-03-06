package org.sitmun.domain;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.common.config.CodeLists;
import org.sitmun.common.domain.cartography.Cartography;
import org.sitmun.common.domain.task.Task;
import org.sitmun.common.domain.user.User;
import org.sitmun.common.types.codelist.CodeList;
import org.sitmun.common.types.http.Http;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Date;

import static org.sitmun.common.config.PersistenceConstants.*;

/**
 * Thematic map.
 */
@Entity
@Table(name = "STM_THEMATIC")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ThematicMap {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_THEMATIC_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "THE_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_THEMATIC_GEN")
  @Column(name = "THE_ID")
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "THE_NAME", length = IDENTIFIER)
  private String name;

  /**
   * Description.
   */
  @Column(name = "THE_DESC", length = SHORT_DESCRIPTION)
  private String description;

  /**
   * Method to create ranges automatically (e.g. unique values, equal record count,
   * equal size interval).
   */
  @Column(name = "THE_RANKTYPE", length = IDENTIFIER)
  @CodeList(CodeLists.THEMATIC_MAP_TYPE)
  private String type;

  /**
   * Number of ranks.
   */
  @Column(name = "THE_RANKNUM")
  @Min(1)
  private Integer ranges;

  /**
   * The start color of the ranges.
   */
  @Column(name = "THE_COLORMIN", length = 250)
  private String startColor;

  /**
   * The end color of the ranges.
   */
  @Column(name = "THE_COLORMAX", length = 250)
  private String endColor;

  /**
   * Border minimum size.
   */
  @Column(name = "THE_SIZEMIN")
  @Min(0)
  private Integer borderMinSize;

  /**
   * Border maximum size.
   */
  @Column(name = "THE_SIZEMAX")
  @Min(0)
  private Integer borderMaxSize;

  /**
   * Opacity.
   */
  @Column(name = "THE_TRANSPARENCY")
  @Min(0)
  @Max(100)
  private Integer transparency;

  /**
   * If <code>true</code>, data from web service can be refreshed.
   */
  @Column(name = "THE_DATAREF")
  private Boolean refreshData;

  /**
   * If <code>true</code>, ranks can be recreated.
   */
  @Column(name = "THE_RANKREC")
  private Boolean recreateRanges;

  /**
   * User.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "THE_USERID", foreignKey = @ForeignKey(name = "STM_THE_FK_USE"))
  private User user;

  /**
   * Cartography.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "THE_GIID", foreignKey = @ForeignKey(name = "STM_THE_FK_GEO"))
  private Cartography cartography;

  /**
   * Task.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "THE_TASKID", foreignKey = @ForeignKey(name = "STM_THE_FK_TAS"))
  private Task task;

  /**
   * Allow to label thematic map.
   */
  @Column(name = "THE_TAGGABLE")
  private Boolean taggable;

  /**
   * Label value type (double or string).
   */
  @Column(name = "THE_VALUETYPE", length = IDENTIFIER)
  @CodeList(CodeLists.THEMATIC_MAP_VALUE_TYPE)
  private String valueType;

  /**
   * Webservice URL that brings data to represent (in JSON format).
   */
  @Column(name = "THE_URLWS", length = URL)
  @Http
  private String url;

  /**
   * If <code>null</code>, the destination of the map is SITMUN.
   */
  @Column(name = "THE_DESTINATION", length = IDENTIFIER)
  @CodeList(CodeLists.THEMATIC_MAP_DESTINATION)
  private String destination;

  /**
   * Expiration date for the thematic map (usually in the case of temporal maps).
   */
  @Column(name = "THE_EXPIRATION")
  @Temporal(TemporalType.TIMESTAMP)
  private Date expirationDate;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof ThematicMap))
      return false;

    ThematicMap other = (ThematicMap) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
