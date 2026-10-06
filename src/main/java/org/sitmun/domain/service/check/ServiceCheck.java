package org.sitmun.domain.service.check;

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
    name = "STM_SERVICE_CHECK",
    uniqueConstraints =
        @UniqueConstraint(
            name = "STM_SEC_UK",
            columnNames = {"SEC_SERID", "SEC_OBSERVER"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceCheck {

  @TableGenerator(
      name = "STM_SERVICE_CHECK_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "SEC_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_SERVICE_CHECK_GEN")
  @Column(name = "SEC_ID")
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "SEC_SERID",
      nullable = false,
      foreignKey = @ForeignKey(name = "STM_SEC_FK_SER"))
  private Service service;

  @Column(name = "SEC_OBSERVER", length = 16, nullable = false)
  private String observer;

  @Column(name = "SEC_STATUS", length = PersistenceConstants.IDENTIFIER, nullable = false)
  private String status;

  @Column(name = "SEC_RANK", nullable = false)
  private Integer statusRank;

  @Column(name = "SEC_OBSERVED", nullable = false)
  private Instant observedAt;

  @Column(name = "SEC_ELAPSED", nullable = false)
  private Long elapsedMs;

  @Column(name = "SEC_DETAIL", length = PersistenceConstants.LONG_DESCRIPTION, nullable = false)
  private String detail;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServiceCheck other)) {
      return false;
    }
    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
