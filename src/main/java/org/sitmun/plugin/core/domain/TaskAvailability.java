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

@Entity
@Table(name = "STM_DISPTAREA", uniqueConstraints = {@UniqueConstraint(name = "STM_DTA_UK", columnNames = {"DTA_CODTER", "DTA_CODTAR"})})
public class TaskAvailability {

  @TableGenerator(
      name = "STM_DISPTAREA_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "DTA_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_DISPTAREA_GEN")
  @Column(name = "DTA_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "DTA_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @ManyToOne
  @JoinColumn(name = "DTA_CODTER", foreignKey = @ForeignKey(name = "STM_DTA_FK_TER"))
  // @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Territory territory;
  @ManyToOne
  @JoinColumn(name = "DTA_CODTAR", foreignKey = @ForeignKey(name = "STM_DTA_FK_TAR"))
  // @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Task task;

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

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

}
