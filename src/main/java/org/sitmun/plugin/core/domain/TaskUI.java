package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Task UI.
 */
@Entity
@Table(name = "STM_TSK_UI")
public class TaskUI {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_TSK_UI_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "TUI_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TSK_UI_GEN")
  @Column(name = "TUI_ID")
  private Integer id;

  /**
   * Task name.
   */
  @Column(name = "TUI_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Tooltip.
   */
  @Column(name = "TUI_TOOLTIP", length = 100)
  private String tooltip;

  /**
   * Task order.
   */
  @Column(name = "TUI_ORDER", precision = 6)
  @Min(0)
  private Integer order;

  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
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

}
