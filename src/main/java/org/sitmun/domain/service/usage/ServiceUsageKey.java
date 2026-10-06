package org.sitmun.domain.service.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceUsageKey implements Serializable {

  @Column(name = "SUS_SERID", nullable = false)
  private Integer serviceId;

  @Column(name = "SUS_APPID", nullable = false)
  private Integer applicationId;

  @Column(name = "SUS_DAY", nullable = false)
  private LocalDate usageDay;

  @Column(name = "SUS_OPERATION", length = 32, nullable = false)
  private String operation;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServiceUsageKey other)) {
      return false;
    }
    return Objects.equals(serviceId, other.serviceId)
        && Objects.equals(applicationId, other.applicationId)
        && Objects.equals(usageDay, other.usageDay)
        && Objects.equals(operation, other.operation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceId, applicationId, usageDay, operation);
  }
}
