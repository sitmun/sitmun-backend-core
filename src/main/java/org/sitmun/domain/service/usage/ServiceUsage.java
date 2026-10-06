package org.sitmun.domain.service.usage;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "STM_SERVICE_USAGE")
@Getter
@Setter
@NoArgsConstructor
public class ServiceUsage {

  @EmbeddedId private ServiceUsageKey id;

  @Column(name = "SUS_REQUESTS", nullable = false)
  private long requests;

  @Column(name = "SUS_FAILED", nullable = false)
  private long failed;
}
