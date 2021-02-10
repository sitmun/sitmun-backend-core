package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Background.
 */
@Entity
@Table(name = "STM_BACKGRD")
public class Background {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_FONDO_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "BAC_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_FONDO_GEN")
  @Column(name = "BAC_ID")
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "BAC_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Description.
   */
  @Column(name = "BAC_DESC", length = 250)
  private String description;

  /**
   * True if it should be considered active by default in applications.
   */
  @Column(name = "BAC_ACTIVE")
  private Boolean active;

  /**
   * Created date.
   */
  @Column(name = "BAC_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Cartography group used as background.
   */
  @ManyToOne
  @JoinColumn(name = "BAC_GGIID", foreignKey = @ForeignKey(name = "STM_FON_FK_GCA"))
  private BackgroundMap cartographyGroup;

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

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public BackgroundMap getCartographyGroup() {
    return cartographyGroup;
  }

  public void setCartographyGroup(BackgroundMap cartographyGroup) {
    this.cartographyGroup = cartographyGroup;
  }

}
