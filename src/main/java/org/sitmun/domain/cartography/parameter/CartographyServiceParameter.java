package org.sitmun.domain.cartography.parameter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;

/** Fixed OGC parameter applied when a proxied cartography is requested. */
@Entity
@Table(name = "STM_PAR_GS")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographyServiceParameter {

  @TableGenerator(
      name = "STM_PAR_GS_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "GS_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PAR_GS_GEN")
  @Column(name = "GS_ID")
  private Integer id;

  @Column(name = "GS_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  private String name;

  @Column(name = "GS_VALUE", length = PersistenceConstants.VALUE)
  private String value;

  @Column(name = "GS_TYPE", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.CARTOGRAPHY_SERVICE_PARAMETER_TYPE)
  @NotNull
  private String type;

  @ManyToOne
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "GS_GEOID", foreignKey = @ForeignKey(name = "STM_GS_FK_GEO"))
  private Cartography cartography;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CartographyServiceParameter other)) {
      return false;
    }
    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
