package org.sitmun.domain.task.group;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import lombok.*;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.infrastructure.persistence.type.i18n.I18n;
import org.sitmun.infrastructure.persistence.type.i18n.I18nListener;

/** Task group. */
@Entity
@EntityListeners(I18nListener.class)
@Table(name = "STM_GRP_TSK")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskGroup {

  /** Unique identifier. */
  @TableGenerator(
      name = "STM_GRP_TSK_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "GTS_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_GRP_TSK_GEN")
  @Column(name = "GTS_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /** Task group name. */
  @Column(name = "GTS_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @I18n
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TaskGroup other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
