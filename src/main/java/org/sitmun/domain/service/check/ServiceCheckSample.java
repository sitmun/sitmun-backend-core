package org.sitmun.domain.service.check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.service.Service;

@Entity
@Table(
    name = "STM_SERVICE_CHECK_SAMPLE",
    indexes = @Index(name = "STM_SCS_IX_SER_OBS", columnList = "SCS_SERID, SCS_OBSERVED"))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceCheckSample {

  @TableGenerator(
      name = "STM_SERVICE_CHECK_SAMPLE_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "SCS_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_SERVICE_CHECK_SAMPLE_GEN")
  @Column(name = "SCS_ID")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "SCS_SERID",
      nullable = false,
      foreignKey = @ForeignKey(name = "STM_SCS_FK_SER"))
  private Service service;

  @Column(name = "SCS_OBSERVER", length = 16, nullable = false)
  private String observer;

  @Column(name = "SCS_STATUS", length = PersistenceConstants.IDENTIFIER, nullable = false)
  private String status;

  @Column(name = "SCS_RANK", nullable = false)
  private Integer statusRank;

  @Column(name = "SCS_ELAPSED", nullable = false)
  private Long elapsedMs;

  @Column(name = "SCS_OBSERVED", nullable = false)
  private Instant observedAt;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServiceCheckSample other)) {
      return false;
    }
    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
