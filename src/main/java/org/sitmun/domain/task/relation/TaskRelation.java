package org.sitmun.domain.task.relation;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.task.Task;

@Entity
@Table(name = "STM_TASKREL")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskRelation {

  /** Unique identifier. */
  @TableGenerator(
      name = "STM_TASKREL_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "TAR_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TASKREL_GEN")
  @Column(name = "TAR_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /** Task owner of the relation. */
  @ManyToOne
  @JoinColumn(name = "TAR_TASKID", foreignKey = @ForeignKey(name = "STM_TAR_FK_TAS"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Task task;

  /** A string that denotes traits or aspects of the relation */
  @Column(name = "TAR_TYPE", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  private String relationType;

  /**
   * The target of the relation.
   *
   * <p>When a task is the target of a relation cannot be deleted.
   */
  @ManyToOne
  @JoinColumn(name = "TAR_TASKRELID", foreignKey = @ForeignKey(name = "STM_TAR_FK_TAS_REL"))
  @NotNull
  private Task relatedTask;
}
