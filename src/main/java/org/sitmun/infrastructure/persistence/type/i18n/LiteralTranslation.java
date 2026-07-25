package org.sitmun.infrastructure.persistence.type.i18n;

import static org.sitmun.domain.PersistenceConstants.LONG_DESCRIPTION;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "STM_LITERAL_TRANSLATION")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LiteralTranslation {

  @TableGenerator(
      name = "STM_LITERAL_TRANSLATION_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "LTR_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_LITERAL_TRANSLATION_GEN")
  @Column(name = "LTR_ID")
  private Integer id;

  @Column(name = "LTR_LITERAL", nullable = false, length = LONG_DESCRIPTION)
  @NotBlank
  @Size(max = LONG_DESCRIPTION)
  private String literal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "LTR_LANID",
      nullable = false,
      foreignKey = @ForeignKey(name = "STM_LTR_FK_LAN"))
  private Language sourceLanguage;
}
