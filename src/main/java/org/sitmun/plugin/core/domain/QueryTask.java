package org.sitmun.plugin.core.domain;

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

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Task getReportTask() {
    return reportTask;
  }

  public void setReportTask(Task reportTask) {
    this.reportTask = reportTask;
  }
}
