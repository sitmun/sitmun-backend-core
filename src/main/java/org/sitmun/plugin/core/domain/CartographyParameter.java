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
 * Geographic Information parameter.
 */
@Entity
@Table(name = "STM_PAR_GI")
public class CartographyParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_PAR_GI_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "PGI_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PAR_GI_GEN")
  @Column(name = "PGI_ID")
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "PGI_NAME", length = 250)
  @NotNull
  private String name;

  /**
   * Value.
   */
  @Column(name = "PGI_VALUE", length = 250)
  @NotNull
  private String value;

  /**
   * Format.
   */
  @Column(name = "PGI_FORMAT", length = 250)
  private String format;

  /**
   * Type.
   */
  @Column(name = "PGI_TYPE", length = 250)
  @NotNull
  private String type;

  /**
   * Cartography that owns this parameter.
   */
  @ManyToOne
  @JoinColumn(name = "PGI_GIID")
  @NotNull
  private Cartography cartography;

  /**
   * Order.
   */
  @Column(name = "PGI_ORDER")
  private Integer order;

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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Cartography getCartography() {
    return cartography;
  }

  public void setCartography(Cartography cartography) {
    this.cartography = cartography;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }
}

