package org.sitmun.domain.cartography.parameter;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.*;
import org.hibernate.Length;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;
import org.sitmun.infrastructure.persistence.type.i18n.I18n;
import org.sitmun.infrastructure.persistence.type.i18n.I18nListener;

/** Geographic Information parameter. */
@Entity
@EntityListeners(I18nListener.class)
@Table(name = "STM_PAR_GI")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographyParameter {

  /** Unique identifier. */
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

  /** Name. */
  @Column(name = "PGI_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  private String name;

  /** Default-language label. Other languages come from STM_TRANSLATION. */
  @Column(name = "PGI_VALUE", length = PersistenceConstants.VALUE)
  @NotNull
  @I18n
  private String value;

  /** Format. */
  @Column(name = "PGI_FORMAT", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.CARTOGRAPHY_PARAMETER_FORMAT)
  private String format;

  /** Type. */
  @Column(name = "PGI_TYPE", length = PersistenceConstants.IDENTIFIER)
  @NotNull
  @CodeList(CodeListsConstants.CARTOGRAPHY_PARAMETER_TYPE)
  private String type;

  /** Cartography that owns this parameter. */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PGI_GIID", foreignKey = @ForeignKey(name = "STM_PGI_FK_GEO"))
  @NotNull
  private Cartography cartography;

  /** Order. */
  @Column(name = "PGI_ORDER")
  @Min(0)
  private Integer order;

  /** Format options for N, P, and F. Empty means the format defaults. */
  @Column(name = "PGI_OPTIONS", length = Length.LONG32)
  @Convert(converter = FeatureInfoFormatOptionsConverter.class)
  @Valid
  private FeatureInfoFormatOptions options;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CartographyParameter other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
