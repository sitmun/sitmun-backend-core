package org.sitmun.plugin.core.domain;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Available languages.
 */
@Entity
@Table(name = "STM_LANGUAGE",
  uniqueConstraints = {@UniqueConstraint(columnNames = {"LAN_SHORTNAME"})})
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
   * Language identifier.
   */
  @Column(name = "LAN_SHORTNAME", length = IDENTIFIER)
  @NotBlank
  private String shortname;

  /**
   * Language name.
   */
  @Column(name = "LAN_NAME", length = 80)
  @NotBlank
  private String name;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getShortname() {
    return shortname;
  }

  public void setShortname(String shortname) {
    this.shortname = shortname;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
