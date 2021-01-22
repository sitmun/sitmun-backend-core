package org.sitmun.plugin.core.domain;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Code list value.
 */
@Entity
@Table(name = "STM_CODELIST",
  uniqueConstraints = {@UniqueConstraint(columnNames = {"COD_LIST", "COD_VALUE"})})
public class CodeListValue {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_CODELIST_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "COD_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CODELIST_GEN")
  @Column(name = "COD_ID")
  private Integer id;

  /**
   * Code list name.
   */
  @Column(name = "COD_LIST", length = IDENTIFIER)
  @NotBlank
  private String codeListName;

  /**
   * Value.
   */
  @Column(name = "COD_VALUE", length = IDENTIFIER)
  @NotBlank
  private String value;

  /**
   * Value description.
   */
  @Column(name = "COD_DESCRIPTION")
  private String description;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCodeListName() {
    return codeListName;
  }

  public void setCodeListName(String codeListName) {
    this.codeListName = codeListName;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
