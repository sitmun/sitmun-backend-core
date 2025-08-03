package org.sitmun.domain.service.parameter;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.service.Service;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;

/** Service parameter. */
@Entity
@Table(name = "STM_PAR_SER")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceParameter {

  /** Unique identifier. */
  @TableGenerator(
      name = "STM_PAR_SER_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "PSE_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PAR_SER_GEN")
  @Column(name = "PSE_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /** Parameter name. */
  @Column(name = "PSE_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /** Parameter value. */
  @Column(name = "PSE_VALUE", length = PersistenceConstants.VALUE)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String value;

  /** Parameter type. */
  @Column(name = "PSE_TYPE", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.SERVICE_PARAMETER_TYPE)
  @NotNull
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String type;

  /** Service that applies this parameter. */
  @ManyToOne
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PSE_SERID", foreignKey = @ForeignKey(name = "STM_PSE_FK_SER"))
  private Service service;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ServiceParameter other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
