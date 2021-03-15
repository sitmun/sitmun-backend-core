package org.sitmun.plugin.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Code list value.
 */
@Entity
@Table(name = "STM_CODELIST",
  uniqueConstraints = {@UniqueConstraint(columnNames = {"COD_LIST", "COD_VALUE"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

  @JsonIgnore
  @Transient
  private String storedCodeListName;

  /**
   * Value.
   */
  @Column(name = "COD_VALUE", length = IDENTIFIER)
  @NotBlank
  private String value;

  @JsonIgnore
  @Transient
  private String storedValue;

  /**
   * Value.
   */
  @Column(name = "COD_SYSTEM")
  private Boolean system;

  @JsonIgnore
  @Transient
  private Boolean storedSystem;

  /**
   * Value description.
   */
  @Column(name = "COD_DESCRIPTION")
  private String description;

  @PostLoad
  public void postLoad() {
    storedCodeListName = codeListName;
    storedValue = value;
    storedSystem = system;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof CodeListValue))
      return false;

    CodeListValue other = (CodeListValue) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
