package org.sitmun.plugin.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * Available translations.
 */
@Entity
@Table(name = "STM_TRANSLATION", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"TRA_ELEID", "TRA_COLUMN", "TRA_LANID"})})
public class Translation {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_TRANSLATION_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "TRA_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TRANSLATION_GEN")
  @Column(name = "TRA_ID")
  private Integer id;

  /**
   * Row identifier.
   */
  @Column(name = "TRA_ELEID")
  @NotNull
  private Integer element;

  /**
   * Table and Column identifier.
   */
  @Column(name = "TRA_COLUMN", length = 30)
  @NotNull
  private String column;

  /**
   * Translation language.
   */
  @ManyToOne
  @JoinColumn(name = "TRA_LANID")
  @NotNull
  private Language language;

  /**
   * Translation.
   */
  @Column(name = "TRA_NAME", length = 250)
  @NotNull
  private String translation;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getElement() {
    return element;
  }

  public void setElement(Integer element) {
    this.element = element;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public String getTranslation() {
    return translation;
  }

  public void setTranslation(String translation) {
    this.translation = translation;
  }
}
