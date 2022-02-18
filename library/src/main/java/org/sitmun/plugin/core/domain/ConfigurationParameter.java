package org.sitmun.plugin.core.domain;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;
import static org.sitmun.plugin.core.domain.Constants.VALUE;

/**
 * Configuration parameter.
 */
@Entity
@Table(name = "STM_CONF",
  uniqueConstraints = @UniqueConstraint(name = "STM_CONF_NAME_UK", columnNames = {"CNF_NAME"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigurationParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_CONF_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "CNF_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CONF_GEN")
  @Column(name = "CNF_ID")
  private Integer id;

  /**
   * Application parameter name.
   */
  @Column(name = "CNF_NAME", length = IDENTIFIER)
  @NotBlank
  @JsonView({Workspace.View.class, WorkspaceApplication.View.class})
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "CNF_VALUE", length = VALUE)
  @JsonView({Workspace.View.class, WorkspaceApplication.View.class})
  private String value;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof ApplicationParameter))
      return false;

    ApplicationParameter other = (ApplicationParameter) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
