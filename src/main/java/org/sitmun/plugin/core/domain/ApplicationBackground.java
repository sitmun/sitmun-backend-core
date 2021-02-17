package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Relationship between applications and backgrounds.
 */
@Entity
@Table(name = "STM_APP_BCKG", uniqueConstraints = {
  @UniqueConstraint(name = "STM_APF_UK", columnNames = {"ABC_APPID", "ABC_BACKID"})})
public class ApplicationBackground {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_APP_BCKG_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "ABC_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APP_BCKG_GEN")
  @Column(name = "ABC_ID")
  private Integer id;

  /**
   * Application.
   */
  @ManyToOne
  @JoinColumn(name = "ABC_APPID", foreignKey = @ForeignKey(name = "STM_APF_FK_APP"))
  @NotNull
  private Application application;

  /**
   * Background.
   */
  @ManyToOne
  @JoinColumn(name = "ABC_BACKID", foreignKey = @ForeignKey(name = "STM_APF_FK_FON"))
  @NotNull
  private Background background;

  /**
   * Order of preference.
   * It can be used for sorting the list of backgrounds in a view.
   */
  @Column(name = "ABC_ORDER", precision = 6)
  private Integer order;


  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Background getBackground() {
    return background;
  }

  public void setBackground(Background background) {
    this.background = background;
  }

}
