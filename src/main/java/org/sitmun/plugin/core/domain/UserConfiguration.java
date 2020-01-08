package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
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
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "STM_USUCONF", uniqueConstraints = {@UniqueConstraint(name = "STM_UCF_UK", columnNames = {"UCF_CODUSU", "UCF_CODTER", "UCF_CODROL"})})
public class UserConfiguration {

  @TableGenerator(
      name = "STM_USUCONF_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "UCF_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_USUCONF_GEN")
  @Column(name = "UCF_CODIGO", precision = 11)
  private BigInteger id;

  @JoinColumn(name = "UCF_CODUSU", foreignKey = @ForeignKey(name = "STM_UCF_FK_USU"))
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  @ManyToOne
  // @MapsId("territorioId")
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "UCF_CODTER", foreignKey = @ForeignKey(name = "STM_UCF_FK_TER"))
  private Territory territory;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  // @MapsId("rolId")
  @JoinColumn(name = "UCF_CODROL", foreignKey = @ForeignKey(name = "STM_UCF_FK_ROL"))
  private Role role;

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
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

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

}
