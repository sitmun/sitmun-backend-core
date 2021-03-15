package org.sitmun.plugin.core.domain;

import lombok.*;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Query task.
 */
@Entity
@Table(name = "STM_QUERY")
@PrimaryKeyJoinColumn(name = "QUE_ID")
@Builder(builderMethodName = "queryBuilder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryTask extends Task {

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
  @Column(name = "QUE_DESC", length = 250)
  private String description;

  /**
   * Report task.
   */
  @ManyToOne
  @JoinColumn(name = "QUE_TASKID")
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
