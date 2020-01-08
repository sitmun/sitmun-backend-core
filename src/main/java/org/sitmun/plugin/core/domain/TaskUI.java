package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "STM_TAREA_UI")
public class TaskUI {

  @TableGenerator(
      name = "STM_TAREA_UI_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "TUI_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TAREA_UI_GEN")
  @Column(name = "TUI_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "TUI_NOMBRE", length = 30)
  private String name;

  @Column(name = "TUI_TOOLTIP", length = 100)
  private String tooltip;

  @Column(name = "TUI_ORDEN", precision = 6)
  private BigInteger order;

  @Column(name = "TUI_TIPO", length = 30)
  private String type;


  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
