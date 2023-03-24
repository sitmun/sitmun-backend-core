package org.sitmun.domain.task.availability;


import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Availability of Tasks in a Territory.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_AVAIL_TSK", uniqueConstraints = {
  @UniqueConstraint(name = "STM_DTA_UK", columnNames = {"ATS_TERID", "ATS_TASKID"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
  @CreatedDate
  private Date createdDate;

  /**
   * Territory allowed to access to the task.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ATS_TERID", foreignKey = @ForeignKey(name = "STM_ATS_FK_TER"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Territory territory;

  /**
   * Task allowed to the territory.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ATS_TASKID", foreignKey = @ForeignKey(name = "STM_ATS_FK_TAS"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Task task;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TaskAvailability))
      return false;

    TaskAvailability other = (TaskAvailability) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
