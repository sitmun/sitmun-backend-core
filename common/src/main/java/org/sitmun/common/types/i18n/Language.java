package org.sitmun.common.types.i18n;

import lombok.*;
import org.sitmun.common.config.PersistenceConstants;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

/**
 * Available languages.
 */
@Entity
@Table(name = "STM_LANGUAGE",
  uniqueConstraints = {@UniqueConstraint(columnNames = {"LAN_SHORTNAME"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(I18nListener.class)
public class Language {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_LANGUAGE_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "LAN_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_LANGUAGE_GEN")
  @Column(name = "LAN_ID")
  private Integer id;

  /**
   * BCP 47 language tag.
   */
  @Column(name = "LAN_SHORTNAME", length = PersistenceConstants.BCP47_LANGUAGE_TAG)
  @NotBlank
  private String shortname;

  /**
   * Language name.
   */
  @Column(name = "LAN_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @I18n
  private String name;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Language))
      return false;

    Language other = (Language) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
