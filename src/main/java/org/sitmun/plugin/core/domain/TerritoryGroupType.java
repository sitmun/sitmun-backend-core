package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Type of grouping of territorial entities.
 */
@Entity
@Table(name = "STM_GTER_TYP", uniqueConstraints = {
  @UniqueConstraint(name = "STM_GTT_NOM_UK", columnNames = {"GTT_NAME"})})
public class TerritoryGroupType {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_GTER_TYP_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "GTT_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GTER_TYP_GEN")
  @Column(name = "GTT_ID")
  private Integer id;

  /**
   * Name.
   */
  @NotBlank
  @Column(name = "GTT_NAME", length = 250)
  private String name;

  public TerritoryGroupType() {
  }

  private TerritoryGroupType(Integer id,
                             @NotNull String name) {
    this.id = id;
    this.name = name;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public static class Builder {
    private Integer id;
    private @NotNull String name;

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setName(@NotBlank String name) {
      this.name = name;
      return this;
    }

    public TerritoryGroupType build() {
      return new TerritoryGroupType(id, name);
    }
  }
}
