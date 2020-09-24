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
import javax.validation.constraints.NotNull;

/**
 * Geographic Information filter.
 */
@Entity
@Table(name = "STM_FIL_GI")
public class CartographyFilter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_FIL_GI_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "FGI_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_FIL_GI_GEN")
  @Column(name = "FGI_ID")
  private Integer id;

  /**
   * Filter name.
   */
  @Column(name = "FGI_NAME", length = 80)
  @NotNull
  private String name;

  /**
   * If <code>true</code>, this filter is required.
   */
  @Column(name = "FGI_REQUIRED")
  @NotNull
  private Boolean required;

  /**
   * Type of filter: custom or required.
   */
  @Column(name = "FGI_TYPE", length = 1)
  @NotNull
  private String type;

  /**
   * Territorial level.
   */
  @ManyToOne
  @JoinColumn(name = "FGI_TYPID")
  @NotNull
  private TerritoryType territorialLevel;

  /**
   * Column where the filter applies.
   */
  @Column(name = "FGI_COLUMN", length = 250)
  private String column;

  /**
   * A row is part of the filter if the value of the column is one of these values.
   */
  @Column(name = "FGI_VALUE", length = 4000)
  private String value;

  /**
   * Type of filter value.
   */
  @Column(name = "FGI_VALUETYPE", length = 30)
  private String valueType;

  /**
   * Cartography on which this filter can be applied.
   */
  @ManyToOne
  @JoinColumn(name = "FGI_GIID")
  @NotNull
  private Cartography cartography;

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

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public TerritoryType getTerritorialLevel() {
    return territorialLevel;
  }

  public void setTerritorialLevel(TerritoryType territorialLevel) {
    this.territorialLevel = territorialLevel;
  }

  public String getColumn() {
    return column;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getValueType() {
    return valueType;
  }

  public void setValueType(String valueType) {
    this.valueType = valueType;
  }

  public Cartography getCartography() {
    return cartography;
  }

  public void setCartography(Cartography cartography) {
    this.cartography = cartography;
  }
}
