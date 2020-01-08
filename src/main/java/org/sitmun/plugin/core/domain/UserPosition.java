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
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "STM_CARGO")
public class UserPosition {

  @TableGenerator(
      name = "STM_CARGO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "CGO_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CARGO_GEN")
  @Column(name = "CGO_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "CGO_CARGO", length = 250)
  private String name;

  @Column(name = "CGO_ORG", length = 250)
  private String organization;

  @Column(name = "CGO_CORREO", length = 250)
  private String email;

  @Column(name = "CGO_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @Column(name = "CGO_F_CADUC")
  @Temporal(TemporalType.TIMESTAMP)
  private Date datedDate;

  @ManyToOne
  @JoinColumn(name = "CGO_CODUSU", foreignKey = @ForeignKey(name = "STM_CGO_FK_USU"))
  @NotNull
  private User user;

  @ManyToOne
  @JoinColumn(name = "CGO_CODTER", foreignKey = @ForeignKey(name = "STM_CGO_FK_TER"))
  @NotNull
  private Territory territory;

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

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Date getDatedDate() {
    return datedDate;
  }

  public void setDatedDate(Date datedDate) {
    this.datedDate = datedDate;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Territory getTerritory() {
    return territory;
  }

  public void setTerritory(Territory territory) {
    this.territory = territory;
  }

}
