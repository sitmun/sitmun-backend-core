package org.sitmun.domain;

import lombok.*;
import org.sitmun.common.config.CodeLists;
import org.sitmun.common.config.PersistenceConstants;
import org.sitmun.common.domain.task.Task;
import org.sitmun.common.types.codelist.CodeList;

import javax.persistence.*;

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
  @Column(name = "QUE_ID")
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
  @Column(name = "QUE_TYPE", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeLists.QUERY_TASK_SCOPE)
  private String scope;

  /**
   * Command description.
   */
  @Column(name = "QUE_DESC", length = PersistenceConstants.SHORT_DESCRIPTION)
  private String description;

  /**
   * Report task.
   */
  @ManyToOne(fetch = FetchType.LAZY)
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
