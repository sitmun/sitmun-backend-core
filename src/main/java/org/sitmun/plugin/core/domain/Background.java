package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
  @Column(name = "BAC_ID", precision = 11)
  private BigInteger id;

  /**
   * Name.
   */
  @Column(name = "BAC_NAME", length = 30)
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
  @NotNull
  private BackgroundMap cartographyGroup;

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
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
