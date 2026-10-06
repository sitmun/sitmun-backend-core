package org.sitmun.domain.service.check;

import java.time.Instant;
import java.util.Date;
import org.sitmun.domain.service.Service;
import org.sitmun.upstream.signal.ServiceStatus;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class ServiceCheckStore {

  public static final String BACKEND = "backend";
  public static final String PROXY = "proxy";

  private final ServiceCheckRepository checkRepository;
  private final ServiceCheckSampleRepository sampleRepository;

  public ServiceCheckStore(
      ServiceCheckRepository checkRepository, ServiceCheckSampleRepository sampleRepository) {
    this.checkRepository = checkRepository;
    this.sampleRepository = sampleRepository;
  }

  @Transactional
  public ServiceCheck record(
      Service service,
      String observer,
      ServiceStatus status,
      long elapsedMs,
      String detail,
      Instant observedAt) {
    ServiceCheck latest =
        checkRepository
            .findByService_IdAndObserver(service.getId(), observer)
            .orElseGet(() -> ServiceCheck.builder().service(service).observer(observer).build());
    latest.setStatus(status.code());
    latest.setStatusRank(status.rank());
    latest.setElapsedMs(elapsedMs);
    latest.setDetail(detail == null ? "" : detail);
    latest.setObservedAt(observedAt);
    ServiceCheck saved = checkRepository.save(latest);
    sampleRepository.save(
        ServiceCheckSample.builder()
            .service(service)
            .observer(observer)
            .status(status.code())
            .statusRank(status.rank())
            .elapsedMs(elapsedMs)
            .observedAt(observedAt)
            .build());
    return saved;
  }

  @Transactional
  public int deleteSamplesOlderThan(Instant cutoff) {
    return sampleRepository.deleteObservedBefore(cutoff);
  }

  @Transactional(readOnly = true)
  public Instant oldestDueAt(Instant dueBefore) {
    Instant observed = checkRepository.oldestDueObservedAt(BACKEND, dueBefore);
    Date created = checkRepository.oldestUnprobedCreated(BACKEND);
    Instant createdAt = created == null ? null : created.toInstant();
    if (observed == null) {
      return createdAt;
    }
    if (createdAt == null) {
      return observed;
    }
    return observed.isBefore(createdAt) ? observed : createdAt;
  }
}
