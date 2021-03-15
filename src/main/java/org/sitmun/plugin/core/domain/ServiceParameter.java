package org.sitmun.plugin.core.domain;


import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

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
  private Integer id;

  /**
   * Parameter name.
   */
  @Column(name = "PSE_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "PSE_VALUE", length = 250)
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PSE_TYPE", length = IDENTIFIER)
  @CodeList(CodeLists.SERVICE_PARAMETER_TYPE)
  @NotNull
  private String type;

  /**
   * Service that applies this parameter.
   */
  @ManyToOne
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
