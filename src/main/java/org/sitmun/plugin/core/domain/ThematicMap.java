package org.sitmun.plugin.core.domain;

import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.constraints.HttpURL;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Date;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

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
  @Column(name = "THE_DESC", length = 250)
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
  @Column(name = "THE_URLWS", length = 250)
  @HttpURL
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

  public ThematicMap() {
  }

  private ThematicMap(Integer id, String name, String description, String type,
                      Integer ranges, String startColor, String endColor,
                      Integer borderMinSize, Integer borderMaxSize,
                      @Min(0) @Max(100) Integer transparency, Boolean refreshData,
                      Boolean recreateRanges, User user,
                      Cartography cartography, Task task, Boolean taggable, String valueType,
                      String url, String destination, Date expirationDate) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.ranges = ranges;
    this.startColor = startColor;
    this.endColor = endColor;
    this.borderMinSize = borderMinSize;
    this.borderMaxSize = borderMaxSize;
    this.transparency = transparency;
    this.refreshData = refreshData;
    this.recreateRanges = recreateRanges;
    this.user = user;
    this.cartography = cartography;
    this.task = task;
    this.taggable = taggable;
    this.valueType = valueType;
    this.url = url;
    this.destination = destination;
    this.expirationDate = expirationDate;
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

  public static class Builder {
    private Integer id;
    private String name;
    private String description;
    private String type;
    private Integer ranges;
    private String startColor;
    private String endColor;
    private Integer borderMinSize;
    private Integer borderMaxSize;
    private @Min(0) @Max(100) Integer transparency;
    private Boolean refreshData;
    private Boolean recreateRanges;
    private User user;
    private Cartography cartography;
    private Task task;
    private Boolean taggable;
    private String valueType;
    private String url;
    private String destination;
    private Date expirationDate;

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setRanges(Integer ranges) {
      this.ranges = ranges;
      return this;
    }

    public Builder setStartColor(String startColor) {
      this.startColor = startColor;
      return this;
    }

    public Builder setEndColor(String endColor) {
      this.endColor = endColor;
      return this;
    }

    public Builder setBorderMinSize(Integer borderMinSize) {
      this.borderMinSize = borderMinSize;
      return this;
    }

    public Builder setBorderMaxSize(Integer borderMaxSize) {
      this.borderMaxSize = borderMaxSize;
      return this;
    }

    public Builder setTransparency(@Min(0) @Max(100) Integer transparency) {
      this.transparency = transparency;
      return this;
    }

    public Builder setRefreshData(Boolean refreshData) {
      this.refreshData = refreshData;
      return this;
    }

    public Builder setRecreateRanges(Boolean recreateRanges) {
      this.recreateRanges = recreateRanges;
      return this;
    }

    public Builder setUser(User user) {
      this.user = user;
      return this;
    }

    public Builder setCartography(Cartography cartography) {
      this.cartography = cartography;
      return this;
    }

    public Builder setTask(Task task) {
      this.task = task;
      return this;
    }

    public Builder setTaggable(Boolean taggable) {
      this.taggable = taggable;
      return this;
    }

    public Builder setValueType(String valueType) {
      this.valueType = valueType;
      return this;
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setDestination(String destination) {
      this.destination = destination;
      return this;
    }

    public Builder setExpirationDate(Date expirationDate) {
      this.expirationDate = expirationDate;
      return this;
    }

    public ThematicMap build() {
      return new ThematicMap(id, name, description, type, ranges, startColor, endColor,
        borderMinSize,
        borderMaxSize, transparency, refreshData, recreateRanges, user, cartography, task,
        taggable,
        valueType, url, destination, expirationDate);
    }
  }
}
