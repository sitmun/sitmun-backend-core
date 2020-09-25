package org.sitmun.plugin.core.domain;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Thematic map.
 */
@Entity
@Table(name = "STM_THEMATIC")
public class ThematicMap {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_THEMATIC_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "THE_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_THEMATIC_GEN")
  @Column(name = "THE_ID", precision = 11)
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "THE_NAME", length = 250)
  private String name;

  /**
   * Description.
   */
  @Column(name = "THE_DESC", length = 250)
  private String description;

  /**
   * Method to create ranges automatically (e.g. unique values, equal record count,
   * equal size interval).
   */
  @Column(name = "THE_RANKTYPE", length = 30)
  private String type;

  /**
   * Number of ranks.
   */
  @Column(name = "THE_RANKNUM")
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
  private Integer borderMinSize;

  /**
   * Border maximum size.
   */
  @Column(name = "THE_SIZEMAX")
  private Integer borderMaxSize;

  /**
   * Opacity.
   */
  @Column(name = "THE_TRANSPARENCY")
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
  @ManyToOne
  @JoinColumn(name = "THE_USERID")
  private User user;

  /**
   * Cartography.
   */
  @ManyToOne
  @JoinColumn(name = "THE_GIID")
  private Cartography cartography;

  /**
   * Task.
   */
  @ManyToOne
  @JoinColumn(name = "THE_TASKID")
  private Task task;

  /**
   * Allow to label thematic map.
   */
  @Column(name = "THE_TAGGABLE", length = 30)
  private Boolean taggable;

  /**
   * Label value type (double or string).
   */
  @Column(name = "THE_VALUETYPE", length = 30)
  private String valueType;

  /**
   * Webservice URL that brings data to represent (in JSON format).
   */
  @Column(name = "THE_URLWS", length = 250)
  private String url;

  /**
   * If <code>null</code>, the destination of the map is SITMUN.
   */
  @Column(name = "THE_DESTINATION", length = 30)
  private String destination;

  /**
   * Expiration date for the thematic map (usually in the case of temporal maps).
   */
  @Column(name = "THE_EXPIRATION")
  @Temporal(TemporalType.TIMESTAMP)
  private Date expirationDate;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getRanges() {
    return ranges;
  }

  public void setRanges(Integer ranges) {
    this.ranges = ranges;
  }

  public String getStartColor() {
    return startColor;
  }

  public void setStartColor(String startColor) {
    this.startColor = startColor;
  }

  public String getEndColor() {
    return endColor;
  }

  public void setEndColor(String endColor) {
    this.endColor = endColor;
  }

  public Integer getBorderMinSize() {
    return borderMinSize;
  }

  public void setBorderMinSize(Integer borderMinSize) {
    this.borderMinSize = borderMinSize;
  }

  public Integer getBorderMaxSize() {
    return borderMaxSize;
  }

  public void setBorderMaxSize(Integer borderMaxSize) {
    this.borderMaxSize = borderMaxSize;
  }

  public Integer getTransparency() {
    return transparency;
  }

  public void setTransparency(Integer transparency) {
    this.transparency = transparency;
  }

  public Boolean getRefreshData() {
    return refreshData;
  }

  public void setRefreshData(Boolean refreshData) {
    this.refreshData = refreshData;
  }

  public Boolean getRecreateRanges() {
    return recreateRanges;
  }

  public void setRecreateRanges(Boolean recreateRanges) {
    this.recreateRanges = recreateRanges;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Cartography getCartography() {
    return cartography;
  }

  public void setCartography(Cartography cartography) {
    this.cartography = cartography;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

  public Boolean getTaggable() {
    return taggable;
  }

  public void setTaggable(Boolean taggable) {
    this.taggable = taggable;
  }

  public String getValueType() {
    return valueType;
  }

  public void setValueType(String valueType) {
    this.valueType = valueType;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }
}
