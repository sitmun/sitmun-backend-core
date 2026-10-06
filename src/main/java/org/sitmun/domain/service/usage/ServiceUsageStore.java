package org.sitmun.domain.service.usage;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.sitmun.proxy.contract.ServiceUsageReport;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceUsageStore {

  private final ServiceUsageRepository repository;

  public ServiceUsageStore(ServiceUsageRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void addReport(ServiceUsageReport report, ZoneId zone) {
    for (org.sitmun.proxy.contract.ServiceUsage usage : report.usages()) {
      add(
          Math.toIntExact(usage.serviceId()),
          Math.toIntExact(usage.applicationId()),
          usage.hourStart().atZone(zone).toLocalDate(),
          UsageOperation.canonical(usage.operation()),
          usage.requests(),
          usage.failed());
    }
  }

  @Transactional
  public void add(
      int serviceId,
      int applicationId,
      LocalDate usageDay,
      String operation,
      long requests,
      long failed) {
    if (requests == 0 && failed == 0) {
      return;
    }
    String canonical = UsageOperation.canonical(operation);
    if (repository.add(serviceId, applicationId, usageDay, canonical, requests, failed) > 0) {
      return;
    }
    ServiceUsage row = new ServiceUsage();
    row.setId(new ServiceUsageKey(serviceId, applicationId, usageDay, canonical));
    row.setRequests(requests);
    row.setFailed(failed);
    try {
      repository.saveAndFlush(row);
    } catch (DataIntegrityViolationException ex) {
      // Two proxy processes can insert the same day key. The loser adds onto the inserted row.
      repository.add(serviceId, applicationId, usageDay, canonical, requests, failed);
    }
  }

  @Transactional
  public int deleteExpired(Instant now, Duration retention, ZoneId zone) {
    LocalDate cutoff = now.atZone(zone).toLocalDate().minusDays(retention.toDays());
    return repository.deleteByUsageDayBefore(cutoff);
  }
}
