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

/**
 * Availability of Tasks in a Territory.
 */
@Entity
@Table(name = "STM_AVAIL_TSK", uniqueConstraints = {
    @UniqueConstraint(name = "STM_DTA_UK", columnNames = {"ATS_TERID", "ATS_TASKID"})})
public class TaskAvailability {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_DISPTAREA_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "ATS_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_DISPTAREA_GEN")
  @Column(name = "ATS_ID", precision = 11)
  private BigInteger id;

  /**
   * Created date.
   */
  @Column(name = "ATS_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Territory allowed to access to the task.
   */
  @ManyToOne
  @JoinColumn(name = "ATS_TERID", foreignKey = @ForeignKey(name = "STM_DTA_FK_TER"))
  @NotNull
  private Territory territory;

  /**
   * Task allowed to the territory.
   */
  @ManyToOne
  @JoinColumn(name = "ATS_TASKID", foreignKey = @ForeignKey(name = "STM_DTA_FK_TAR"))
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
