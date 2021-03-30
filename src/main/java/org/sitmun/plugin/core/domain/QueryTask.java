package org.sitmun.plugin.core.domain;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;
import static org.sitmun.plugin.core.domain.Constants.SHORT_DESCRIPTION;

/**
 * Query task.
 */
@Entity
@Table(name = "STM_QUERY")
@Builder(builderMethodName = "queryBuilder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryTask {

  @Id
  @JoinColumn(name = "QUE_ID", foreignKey = @ForeignKey(name = "STM_QUE_FK_TAS"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Integer id;

  /**
   * Command depending on the {@link #scope} (a SQL query sentence, a URL,
   * a call to a SITMUN task,...).
   */
  @Column(name = "QUE_COMMAND", length = 250)
  private String command;

  /**
   * Command scope.
   */
  @Column(name = "QUE_TYPE", length = IDENTIFIER)
  @CodeList(CodeLists.QUERY_TASK_SCOPE)
  private String scope;

  /**
   * Command description.
   */
  @Column(name = "QUE_DESC", length = SHORT_DESCRIPTION)
  private String description;

  /**
   * Report task.
   */
  @ManyToOne
  @JoinColumn(name = "QUE_TASKID", foreignKey = @ForeignKey(name = "STM_QUE_FK_TASM"))
  private Task reportTask;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof QueryTask))
      return false;

    QueryTask other = (QueryTask) o;

    return getId() != null &&
      getId().equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
