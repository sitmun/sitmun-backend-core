package org.sitmun.domain.task.parameter;


import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.task.Task;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static org.sitmun.domain.PersistenceConstants.IDENTIFIER;

/**
 * Task parameter.
 */
@Entity
@Table(name = "STM_PAR_TSK")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_PAR_TSK_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "PTT_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PAR_TSK_GEN")
  @Column(name = "PTT_ID")
  private Integer id;

  /**
   * Parameter name.
   */
  @Column(name = "PTT_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "PTT_VALUE", length = 4000)
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PTT_TYPE", length = IDENTIFIER)
  @CodeList(CodeListsConstants.TASK_PARAMETER_TYPE)
  private String type;

  /**
   * Parameter position.
   */
  @Column(name = "PTT_ORDER", precision = 6)
  @Min(0)
  private Integer order;

  /**
   * Attribute format (when editing).
   */
  @Column(name = "PTT_FORMAT", length = IDENTIFIER)
  @CodeList(CodeListsConstants.TASK_PARAMETER_FORMAT)
  private String format;

  /**
   * Description of the meaning of this parameter.
   * Intended to be used in help text and tooltips for final users..
   */
  @Column(name = "PTT_HELP", length = 250)
  private String help;

  /**
   * Content dependent of the parameter format..
   */
  @Column(name = "PTT_SELECT", length = 1500)
  private String select;

  /**
   * If <code>true</code>, this parameter can be used for select content.
   */
  @Column(name = "PTT_SELECTABL")
  private Boolean selectable;

  /**
   * If <code>true</code>, this parameter can be used for select content.
   */
  @Column(name = "PTT_EDITABLE")
  private Boolean editable;

  /**
   * If <code>true</code>, this parameter can be used for select content.
   */
  @Column(name = "PTT_REQUIRED")
  private Boolean required;

  /**
   * Default literal value (for some formats).
   */
  @Column(name = "PTT_DEFAULT", length = 250)
  private String defaultValue;

  /**
   * Maximum length of the value (for some formats).
   */
  @Column(name = "PTT_MAXLEN")
  @Min(1)
  private Integer maxLength;

  /**
   * Specifies which fields participate in a join to another table (for some formats).
   */
  @Column(name = "PTT_VALUEREL", length = 512)
  private String relationAttributes;

  /**
   * Specifies which join fields participate also in the where (for some formats).
   */
  @Column(name = "PTT_FILTERREL", length = 512)
  private String relationAttributesRole;

  /**
   * Tasks that applies this parameter.
   */
  @ManyToOne
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PTT_TASKID", foreignKey = @ForeignKey(name = "STM_PTT_FK_TAS"))
  private Task task;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TaskParameter))
      return false;

    TaskParameter other = (TaskParameter) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
