package org.sitmun.plugin.core.domain;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Available translations.
 */
@Entity
@Table(name = "STM_TRANSLATION", uniqueConstraints = {
  @UniqueConstraint(columnNames = {"TRA_ELEID", "TRA_COLUMN", "TRA_LANID"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Translation {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_TRANSLATION_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
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
  @Column(name = "TRA_COLUMN", length = IDENTIFIER)
  @NotBlank
  private String column;

  /**
   * Translation language.
   */
  @ManyToOne
  @JoinColumn(name = "TRA_LANID", foreignKey = @ForeignKey(name = "STM_TRA_FK_LAN"))
  @NotNull
  private Language language;

  /**
   * Translation.
   */
  @Column(name = "TRA_NAME", length = 250)
  @NotBlank
  private String translation;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Translation))
      return false;

    Translation other = (Translation) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
