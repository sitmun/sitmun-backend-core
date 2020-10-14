package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
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
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

/**
 * Task parameter.
 */
@Entity
@Table(name = "STM_PAR_TSK")
public class TaskParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_PARAMTTA_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "PTT_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PARAMTTA_GEN")
  @Column(name = "PTT_ID", precision = 11)
  private BigInteger id;

  /**
   * Parameter name.
   */
  @Column(name = "PTT_NAME", length = 50)
  @NotBlank
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "PTT_VALUE", length = 512)
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PTT_TYPE", length = 30)
  @CodeList(CodeLists.TASK_PARAMETER_TYPE)
  private String type;

  /**
   * Parameter position.
   */
  @Column(name = "PTT_ORDER", precision = 6)
  @Min(0)
  private BigInteger order;

  /**
   * Attribute format (when editing).
   */
  @Column(name = "PTT_FORMAT", length = 250)
  @CodeList(CodeLists.TASK_PARAMETER_FORMAT)
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
  @JoinColumn(name = "PTT_TASKID", foreignKey = @ForeignKey(name = "STM_PTT_FK_TAR"))
  private Task task;

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getHelp() {
    return help;
  }

  public void setHelp(String help) {
    this.help = help;
  }

  public String getSelect() {
    return select;
  }

  public void setSelect(String select) {
    this.select = select;
  }

  public Boolean getSelectable() {
    return selectable;
  }

  public void setSelectable(Boolean selectable) {
    this.selectable = selectable;
  }

  public Boolean getEditable() {
    return editable;
  }

  public void setEditable(Boolean editable) {
    this.editable = editable;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public Integer getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(Integer maxLength) {
    this.maxLength = maxLength;
  }

  public String getRelationAttributes() {
    return relationAttributes;
  }

  public void setRelationAttributes(String relationAttributes) {
    this.relationAttributes = relationAttributes;
  }

  public String getRelationAttributesRole() {
    return relationAttributesRole;
  }

  public void setRelationAttributesRole(String relationAttributesRole) {
    this.relationAttributesRole = relationAttributesRole;
  }

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }
}
