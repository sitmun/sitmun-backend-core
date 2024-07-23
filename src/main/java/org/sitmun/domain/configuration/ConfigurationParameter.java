package org.sitmun.domain.configuration;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.parameter.ApplicationParameter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

/**
 * Configuration parameter.
 */
@Entity
@Table(name = "STM_CONF",
  uniqueConstraints = @UniqueConstraint(name = "STM_CONF_NAME_UK", columnNames = "CNF_NAME"))
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
  @Column(name = "CNF_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView({ClientConfigurationViews.Base.class, ClientConfigurationViews.ApplicationTerritory.class})
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "CNF_VALUE", length = PersistenceConstants.VALUE)
  @JsonView({ClientConfigurationViews.Base.class, ClientConfigurationViews.ApplicationTerritory.class})
  private String value;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (!(obj instanceof ApplicationParameter)) {
        return false;
    }

    ApplicationParameter other = (ApplicationParameter) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
