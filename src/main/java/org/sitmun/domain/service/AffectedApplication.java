package org.sitmun.domain.service;

import java.util.List;

public record AffectedApplication(
    Integer applicationId,
    String applicationName,
    List<FailingService> failingServices,
    long requests30d,
    AffectedUsage usage) {

  public AffectedApplication {
    if (applicationId == null
        || applicationName == null
        || failingServices == null
        || failingServices.isEmpty()
        || usage == null) {
      throw new IllegalArgumentException("affected application");
    }
    AffectedUsage derived = requests30d > 0 ? AffectedUsage.used : AffectedUsage.configured;
    if (requests30d < 0 || usage != derived) {
      throw new IllegalArgumentException("usage");
    }
    failingServices = List.copyOf(failingServices);
  }

  public record FailingService(Integer id, String name) {}

  public enum AffectedUsage {
    used,
    configured
  }
}
