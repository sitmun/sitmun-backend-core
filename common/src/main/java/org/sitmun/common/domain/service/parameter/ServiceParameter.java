package org.sitmun.common.domain.service.parameter;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.common.config.CodeLists;
import org.sitmun.common.config.PersistenceConstants;
import org.sitmun.common.domain.service.Service;
import org.sitmun.common.types.codelist.CodeList;
import org.sitmun.common.views.Views;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Service parameter.
 */
@Entity
@Table(name = "STM_PAR_SER")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceParameter {

  /**
   * Unique identifier.
   */
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
  @JsonView(Views.WorkspaceApplication.class)
  private Integer id;

  /**
   * Parameter name.
   */
  @Column(name = "PSE_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(Views.WorkspaceApplication.class)
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "PSE_VALUE", length = PersistenceConstants.VALUE)
  @JsonView(Views.WorkspaceApplication.class)
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PSE_TYPE", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeLists.SERVICE_PARAMETER_TYPE)
  @NotNull
  @JsonView(Views.WorkspaceApplication.class)
  private String type;

  /**
   * Service that applies this parameter.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PSE_SERID", foreignKey = @ForeignKey(name = "STM_PSE_FK_SER"))
  private Service service;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof ServiceParameter))
      return false;

    ServiceParameter other = (ServiceParameter) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
