package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

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
    name = "STM_AVAIL_TSK_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "ATS_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_AVAIL_TSK_GEN")
  @Column(name = "ATS_ID")
  private Integer id;

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

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
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
