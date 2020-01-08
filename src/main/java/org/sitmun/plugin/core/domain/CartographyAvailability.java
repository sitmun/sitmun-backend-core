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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "STM_DISPCARTO", uniqueConstraints = {@UniqueConstraint(name = "STM_DCA_UK", columnNames = {"DCA_CODTER", "DCA_CODCAR"})})
public class CartographyAvailability {

  @TableGenerator(
      name = "STM_DISPCARTO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "DCA_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_DISPCARTO_GEN")
  @Column(name = "DCA_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "DCA_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @ManyToOne
  // @MapsId("territorioId")
  @JoinColumn(name = "DCA_CODTER", foreignKey = @ForeignKey(name = "STM_DCA_FK_TER"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Territory territory;

  @ManyToOne
  // @MapsId("cartografiaId")
  @JoinColumn(name = "DCA_CODCAR", foreignKey = @ForeignKey(name = "STM_DCA_FK_CAR"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Cartography cartography;

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

  public String toString() {
    return "Cartography=" + this.cartography.getId() + ",Territorio=" + this.territory.getId() + "fechaAlta="
               + this.createdDate;
  }

}
