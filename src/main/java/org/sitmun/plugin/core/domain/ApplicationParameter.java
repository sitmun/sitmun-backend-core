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
 * Application parameter.
 */
@Entity
@Table(name = "STM_PAR_APP")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_PAR_APP_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "PAP_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PAR_APP_GEN")
  @Column(name = "PAP_ID")
  private Integer id;

  /**
   * Application parameter name.
   */
  @Column(name = "PAP_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "PAP_VALUE", length = 250)
  @NotNull
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PAP_TYPE", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.APPLICATION_PARAMETER_TYPE)
  private String type;

  /**
   * Application that applies this parameter.
   */
  @ManyToOne
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PAP_APPID", foreignKey = @ForeignKey(name = "STM_PAP_FK_APP"))
  private Application application;

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
