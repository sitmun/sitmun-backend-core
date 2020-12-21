package org.sitmun.plugin.core.domain;


import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;


import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
import org.sitmun.plugin.core.converters.StringListAttributeConverter;

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
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "FGI_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_FIL_GI_GEN")
  @Column(name = "FGI_ID")
  private Integer id;

  /**
   * Filter name.
   */
  @Column(name = "FGI_NAME", length = IDENTIFIER)
  @NotBlank
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
  @Column(name = "FGI_TYPE", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.CARTOGRAPHY_FILTER_TYPE)
  private String type;

  /**
   * Territorial level.
   */
  @ManyToOne
  @JoinColumn(name = "FGI_TYPID")
  private TerritoryType territorialLevel;

  /**
   * Column where the filter applies.
   */
  @Column(name = "FGI_COLUMN", length = IDENTIFIER)
  private String column;

  /**
   * A row is part of the filter if the value of the column is one of these values.
   */
  @Column(name = "FGI_VALUE", length = 4000)
  @Convert(converter = StringListAttributeConverter.class)
  private List<String> values;

  /**
   * Type of filter value.
   */
  @Column(name = "FGI_VALUETYPE", length = IDENTIFIER)
  @CodeList(CodeLists.CARTOGRAPHY_FILTER_VALUE_TYPE)
  private String valueType;

  /**
   * Cartography on which this filter can be applied.
   */
  @ManyToOne
  @JoinColumn(name = "FGI_GIID")
  @NotNull
  @JsonIgnore
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

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> value) {
    this.values = value;
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
