package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.sitmun.domain.PersistenceConstants;

import java.util.Objects;

/**
 * Available languages.
 */
@Entity
@Table(name = "STM_LANGUAGE", uniqueConstraints = @UniqueConstraint(columnNames = "LAN_SHORTNAME"))
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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Language other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
