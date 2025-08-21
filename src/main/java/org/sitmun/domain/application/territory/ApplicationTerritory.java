package org.sitmun.domain.application.territory;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.persistence.type.boundingbox.BoundingBox;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;
import org.sitmun.infrastructure.persistence.type.envelope.EnvelopeToStringConverter;

/** Relationship between applications and territories. */
@Entity
@Table(
    name = "STM_APP_TER",
    uniqueConstraints =
        @UniqueConstraint(
            name = "STM_APT_UK",
            columnNames = {"ATE_APPID", "ATE_TERID"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationTerritory {

  /** Unique identifier. */
  @TableGenerator(
      name = "STM_APP_TER_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "ATE_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APP_TER_GEN")
  @Column(name = "ATE_ID")
  private Integer id;

  /** Application. */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "ATE_APPID", foreignKey = @ForeignKey(name = "STM_ATE_FK_APP"))
  @NotNull
  private Application application;

  /** Background. */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "ATE_TERID", foreignKey = @ForeignKey(name = "STM_ATE_FK_TER"))
  @NotNull
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Territory territory;

  /** Initial extension of the application on the territory. */
  @Column(name = "ATE_INIEXT", length = 250)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Convert(converter = EnvelopeToStringConverter.class)
  @BoundingBox
  private Envelope initialExtent;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ApplicationTerritory other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
