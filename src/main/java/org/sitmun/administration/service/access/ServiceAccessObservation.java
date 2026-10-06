package org.sitmun.administration.service.access;

import java.time.Instant;
import org.sitmun.domain.service.check.ServiceCheck;

public record ServiceAccessObservation(
    String status,
    int statusRank,
    String observer,
    long elapsedMs,
    Instant observedAt,
    String detail) {

  public static ServiceAccessObservation from(ServiceCheck check) {
    return new ServiceAccessObservation(
        check.getStatus(),
        check.getStatusRank(),
        check.getObserver(),
        check.getElapsedMs(),
        check.getObservedAt(),
        check.getDetail());
  }
}
