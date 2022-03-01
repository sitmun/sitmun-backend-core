package org.sitmun.domain;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.config.PersistenceConstants;
import org.sitmun.constraints.CodeList;
import org.sitmun.constraints.CodeLists;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Geographic Information parameter.
 */
@Entity
@Table(name = "STM_PAR_GI")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographyParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_PAR_GI_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "PGI_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PAR_GI_GEN")
  @Column(name = "PGI_ID")
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "PGI_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Value.
   */
  @Column(name = "PGI_VALUE", length = PersistenceConstants.VALUE)
  @NotNull
  private String value;

  /**
   * Format.
   */
  @Column(name = "PGI_FORMAT", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeLists.CARTOGRAPHY_PARAMETER_FORMAT)
  private String format;

  /**
   * Type.
   */
  @Column(name = "PGI_TYPE", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  @CodeList(CodeLists.CARTOGRAPHY_PARAMETER_TYPE)
  private String type;

  /**
   * Cartography that owns this parameter.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PGI_GIID", foreignKey = @ForeignKey(name = "STM_PGI_FK_GEO"))
  @NotNull
  private Cartography cartography;

  /**
   * Order.
   */
  @Column(name = "PGI_ORDER")
  @Min(0)
  private Integer order;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof CartographyParameter))
      return false;

    CartographyParameter other = (CartographyParameter) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

