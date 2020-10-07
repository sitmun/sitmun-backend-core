package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Convert;
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
import org.sitmun.plugin.core.converters.StringListAttributeConverter;

/**
 * Log.
 */
@Entity
@Table(name = "STM_LOG")
public class Log {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_LOG_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "LOG_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_LOG_GEN")
  @Column(name = "LOG_ID", precision = 11)
  private BigInteger id;

  /**
   * Action date.
   */
  @Column(name = "LOG_DATE")
  @Temporal(TemporalType.TIMESTAMP)
  private Date date;

  /**
   * Action type.
   */
  @Column(name = "LOG_TYPE", length = 50)
  private String type;

  /**
   * Originated by this user.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_USERID")
  private User user;

  /**
   * Originated by this application.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_APPID")
  private Application application;

  /**
   * Originated in this application.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_TERID")
  private Territory territory;

  /**
   * Originated by this task.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_TASKID")
  private Task task;

  /**
   * Originated by this cartography.
   */
  @ManyToOne
  @JoinColumn(name = "LOG_GIID")
  private Cartography cartography;

  /**
   * Counter (to add up).
   */
  @Column(name = "LOG_COUNT", precision = 11)
  private BigInteger counter;

  /**
   * Territory code.
   */
  @Column(name = "LOG_TER", length = 250)
  private String territoryCode;

  /**
   * If the log entry involves more than one territory, this field must contain a list of all the
   * territories involved.
   */
  @Column(name = "LOG_TEREXT", length = 250)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> territories;

  /**
   * Data (or process) requested.
   */
  @Column(name = "LOG_DATA", length = 250)
  private String data;

  /**
   * SRS requested.
   */
  @Column(name = "LOG_SRS", length = 250)
  private String srs;

  /**
   * Format requested.
   */
  @Column(name = "LOG_FORMAT", length = 250)
  private String format;

  /**
   * True if the user used the add buffer option.
   */
  @Column(name = "LOG_BUFFER")
  private Boolean buffer;

  /**
   * Email where the results have been sent.
   */
  @Column(name = "LOG_EMAIL", length = 250)
  private String email;

  /**
   * Other information.
   */
  @Column(name = "LOG_OTHER", length = 4000)
  private String other;

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Territory getTerritory() {
    return territory;
  }

  public void setTerritory(Territory territory) {
    this.territory = territory;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

  public Cartography getCartography() {
    return cartography;
  }

  public void setCartography(Cartography cartography) {
    this.cartography = cartography;
  }

  public BigInteger getCounter() {
    return counter;
  }

  public void setCounter(BigInteger counter) {
    this.counter = counter;
  }

  public String getTerritoryCode() {
    return territoryCode;
  }

  public void setTerritoryCode(String territoryCode) {
    this.territoryCode = territoryCode;
  }

  public List<String> getTerritories() {
    return territories;
  }

  public void setTerritories(List<String> territoryExtent) {
    this.territories = territoryExtent;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getSrs() {
    return srs;
  }

  public void setSrs(String srs) {
    this.srs = srs;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public Boolean getBuffer() {
    return buffer;
  }

  public void setBuffer(Boolean buffer) {
    this.buffer = buffer;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getOther() {
    return other;
  }

  public void setOther(String other) {
    this.other = other;
  }
}
