package org.sitmun.plugin.core.domain;

import lombok.*;
import org.sitmun.plugin.core.i18n.I18n;
import org.sitmun.plugin.core.i18n.InternationalizationListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.BCP47_LANGUAGE_TAG;

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
@EntityListeners(InternationalizationListener.class)
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
  @Column(name = "LAN_SHORTNAME", length = BCP47_LANGUAGE_TAG)
  @NotBlank
  private String shortname;

  /**
   * Language name.
   */
  @Column(name = "LAN_NAME", length = 80)
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
