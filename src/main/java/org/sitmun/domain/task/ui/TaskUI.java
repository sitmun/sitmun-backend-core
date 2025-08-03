package org.sitmun.domain.task.ui;

import static org.sitmun.domain.PersistenceConstants.IDENTIFIER;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.task.type.TaskType;

/** Task UI. */
@Entity
@Table(name = "STM_TSK_UI")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskUI {

  /** Unique identifier. */
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
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /** Task name. */
  @Column(name = "TUI_NAME", length = IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /** Tooltip. */
  @Column(name = "TUI_TOOLTIP", length = 100)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String tooltip;

  /** Type. */
  @Column(name = "TUI_TYPE", length = 30)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String type;

  /** Task order. */
  @Column(name = "TUI_ORDER", precision = 6)
  @Min(0)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer order;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TaskType other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
