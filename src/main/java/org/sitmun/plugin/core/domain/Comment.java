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
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

/**
 * Comment or user's suggestion.
 */
@Entity
@Table(name = "STM_COMMENT")
public class Comment {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_COMMENT_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "COM_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_COMMENT_GEN")
  @Column(name = "COM_ID")
  private Integer id;

  /**
   * Coordinate X of the place.
   */
  @Column(name = "COM_COORD_X")
  @NotNull
  private Double coordinateX;

  /**
   * Coordinate Y of the place.
   */
  @Column(name = "COM_COORD_Y")
  @NotNull
  private Double coordinateY;

  /**
   * Name of the person.
   */
  @Column(name = "COM_NAME", length = 250)
  private String name;

  /**
   * Email to receive an answer.
   */
  @Column(name = "COM_EMAIL", length = 250)
  @Email
  private String email;

  /**
   * Title of the suggestion.
   */
  @Column(name = "COM_TITLE", length = 500)
  private String title;

  /**
   * Description.
   */
  @Column(name = "COM_DESC", length = 1000)
  private String description;

  /**
   * Creation date.
   */
  @Column(name = "COM_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * User logged.
   */
  @ManyToOne
  @JoinColumn(name = "COM_USERID")
  private User user;

  /**
   * Application used.
   */
  @ManyToOne
  @JoinColumn(name = "COM_APPID")
  private Application application;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Double getCoordinateX() {
    return coordinateX;
  }

  public void setCoordinateX(Double coordinateX) {
    this.coordinateX = coordinateX;
  }

  public Double getCoordinateY() {
    return coordinateY;
  }

  public void setCoordinateY(Double coordinateY) {
    this.coordinateY = coordinateY;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
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
}
