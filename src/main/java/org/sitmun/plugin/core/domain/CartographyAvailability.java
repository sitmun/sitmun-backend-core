package org.sitmun.plugin.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Grants availability of a Geographic Information in a Territory.
 */
@Entity
@Table(name = "STM_AVAIL_GI", uniqueConstraints = {
    @UniqueConstraint(name = "STM_DCA_UK", columnNames = {"AGI_TERID", "AGI_GIID"})})
public class CartographyAvailability {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_DISPCARTO_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "AGI_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_DISPCARTO_GEN")
  @Column(name = "AGI_ID", precision = 11)
  private BigInteger id;

  /**
   * Creation date.
   */
  @Column(name = "AGI_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Owner of the Geographic Information.
   * Keeps the owner's name when ownership is not obvious or is an exception.
   */
  @Column(name = "AGI_PROPRIETA")
  private String owner;

  /**
   * Territory allowed to access to the cartography.
   */
  @ManyToOne
  @JoinColumn(name = "AGI_TERID", foreignKey = @ForeignKey(name = "STM_DCA_FK_TER"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Territory territory;

  /**
   * Cartography allowed to the territory.
   */
  @ManyToOne
  @JoinColumn(name = "AGI_GIID", foreignKey = @ForeignKey(name = "STM_DCA_FK_CAR"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  @JsonIgnore
  private Cartography cartography;

  /**
   * Provides a human readable description of the grant.
   *
   * @return a short description of the grant
   */
  public String toString() {
    return "Cartography=" + this.cartography.getId()
        + ",Territorio=" + this.territory.getId()
        + "fechaAlta=" + this.createdDate;
  }

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Territory getTerritory() {
    return territory;
  }

  public void setTerritory(Territory territory) {
    this.territory = territory;
  }

  public Cartography getCartography() {
    return cartography;
  }

  public void setCartography(Cartography cartography) {
    this.cartography = cartography;
  }
}
