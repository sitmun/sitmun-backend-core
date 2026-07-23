package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "STM_LITERAL_TRANSLATION_VALUE",
    uniqueConstraints = @UniqueConstraint(columnNames = {"LTV_LTRID", "LTV_LANID"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LiteralTranslationValue {

  @TableGenerator(
      name = "STM_LITERAL_TRANSLATION_VALUE_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "LTV_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_LITERAL_TRANSLATION_VALUE_GEN")
  @Column(name = "LTV_ID")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "LTV_LTRID",
      nullable = false,
      foreignKey = @ForeignKey(name = "STM_LTV_FK_LTR"))
  private LiteralTranslation literalTranslation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "LTV_LANID",
      nullable = false,
      foreignKey = @ForeignKey(name = "STM_LTV_FK_LAN"))
  private Language language;

  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(name = "LTV_VALUE", nullable = false, length = Length.LOB_DEFAULT)
  @NotBlank
  private String value;
}
