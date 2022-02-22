package org.sitmun.domain;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import static org.sitmun.domain.Constants.IDENTIFIER;

/**
 * Task UI.
 */
@Entity
@Table(name = "STM_TSK_UI")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
  @JsonView(WorkspaceApplication.View.class)
  private Integer id;

  /**
   * Task name.
   */
  @Column(name = "TUI_NAME", length = IDENTIFIER)
  @NotBlank
  @JsonView(WorkspaceApplication.View.class)
  private String name;

  /**
   * Tooltip.
   */
  @Column(name = "TUI_TOOLTIP", length = 100)
  @JsonView(WorkspaceApplication.View.class)
  private String tooltip;

  /**
   * Task order.
   */
  @Column(name = "TUI_ORDER", precision = 6)
  @Min(0)
  @JsonView(WorkspaceApplication.View.class)
  private Integer order;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TaskType))
      return false;

    TaskType other = (TaskType) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
