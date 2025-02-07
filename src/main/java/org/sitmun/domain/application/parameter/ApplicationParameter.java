package org.sitmun.domain.application.parameter;


import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

import static org.sitmun.domain.PersistenceConstants.IDENTIFIER;
import static org.sitmun.domain.PersistenceConstants.VALUE;

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
  @Column(name = "PAP_VALUE", length = VALUE)
  @NotNull
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PAP_TYPE", length = IDENTIFIER)
  @NotNull
  @CodeList(CodeListsConstants.APPLICATION_PARAMETER_TYPE)
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
