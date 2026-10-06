package org.sitmun.administration.service.access;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.check.ServiceCheck;
import org.sitmun.domain.service.check.ServiceCheckRepository;
import org.sitmun.domain.service.check.ServiceCheckSample;
import org.sitmun.domain.service.check.ServiceCheckSampleRepository;
import org.sitmun.domain.service.usage.ServiceUsageQueries;
import org.sitmun.domain.service.usage.UsageWindow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceAccessSummaries {

  static final int HOURS = 24;
  static final int TEN_MINUTES = 10;
  static final int TEN_MINUTE_BUCKETS = 24 * (60 / TEN_MINUTES);

  private final ServiceRepository services;
  private final ServiceCheckRepository checks;
  private final ServiceCheckSampleRepository samples;
  private final ServiceUsageQueries usage;

  public ServiceAccessSummaries(
      ServiceRepository services,
      ServiceCheckRepository checks,
      ServiceCheckSampleRepository samples,
      ServiceUsageQueries usage) {
    this.services = services;
    this.checks = checks;
    this.samples = samples;
    this.usage = usage;
  }

  @Transactional(readOnly = true)
  public List<ServiceAccessSummary> list(Instant now) {
    Instant currentHour = now.truncatedTo(ChronoUnit.HOURS);
    Instant from = currentHour.minus(HOURS - 1L, ChronoUnit.HOURS);
    Instant to = currentHour.plus(1, ChronoUnit.HOURS);

    Map<Integer, List<ServiceCheck>> latestByService = new HashMap<>();
    for (ServiceCheck check : checks.findAll()) {
      latestByService
          .computeIfAbsent(check.getService().getId(), id -> new ArrayList<>())
          .add(check);
    }

    Map<Integer, PaintedBucket[]> buckets = new HashMap<>();
    for (ServiceCheckSample sample :
        samples.findByObservedAtGreaterThanEqualAndObservedAtLessThan(from, to)) {
      int index =
          (int)
              ChronoUnit.HOURS.between(from, sample.getObservedAt().truncatedTo(ChronoUnit.HOURS));
      if (index < 0 || index >= HOURS) {
        continue;
      }
      PaintedBucket[] row =
          buckets.computeIfAbsent(sample.getService().getId(), id -> new PaintedBucket[HOURS]);
      if (row[index] == null) {
        row[index] = new PaintedBucket();
      }
      row[index].add(sample);
    }

    Map<Integer, Boolean> proxied = new HashMap<>();
    for (Map.Entry<Integer, List<ServiceCheck>> entry : latestByService.entrySet()) {
      proxied.put(entry.getKey(), entry.getValue().get(0).getService().getIsProxied());
    }
    Map<Integer, UsageWindow> usage30 = usage.windows(latestByService.keySet(), proxied, now);

    List<ServiceAccessSummary> summaries = new ArrayList<>();
    for (Map.Entry<Integer, List<ServiceCheck>> entry : latestByService.entrySet()) {
      ServiceCheck newer = newer(entry.getValue());
      PaintedBucket[] row = buckets.get(entry.getKey());
      summaries.add(
          new ServiceAccessSummary(
              entry.getKey(),
              newer.getStatus(),
              newer.getStatusRank(),
              newer.getObserver(),
              newer.getElapsedMs(),
              newer.getObservedAt(),
              newer.getDetail(),
              hours(row),
              usage30.get(entry.getKey())));
    }
    summaries.sort(Comparator.comparing(ServiceAccessSummary::serviceId));
    return summaries;
  }

  @Transactional(readOnly = true)
  public List<ServiceAccessHour> tenMinuteBuckets(Integer serviceId, Instant now) {
    Instant current = truncateToTenMinutes(now);
    Instant from = current.minus((TEN_MINUTE_BUCKETS - 1L) * TEN_MINUTES, ChronoUnit.MINUTES);
    Instant to = current.plus(TEN_MINUTES, ChronoUnit.MINUTES);
    PaintedBucket[] row = new PaintedBucket[TEN_MINUTE_BUCKETS];
    for (ServiceCheckSample sample :
        samples.findByService_IdAndObservedAtGreaterThanEqualAndObservedAtLessThan(
            serviceId, from, to)) {
      int index =
          (int)
              (ChronoUnit.MINUTES.between(from, truncateToTenMinutes(sample.getObservedAt()))
                  / TEN_MINUTES);
      if (index < 0 || index >= TEN_MINUTE_BUCKETS) {
        continue;
      }
      if (row[index] == null) {
        row[index] = new PaintedBucket();
      }
      row[index].add(sample);
    }
    return hours(row);
  }

  @Transactional(readOnly = true)
  public ServiceAccessSamples samples(
      Integer serviceId, Instant now, Duration timeout, Duration retention) {
    if (!services.existsById(serviceId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    Instant from = now.minus(retention);
    List<ServiceAccessSample> rows = new ArrayList<>();
    for (ServiceCheckSample sample :
        samples
            .findByService_IdAndObservedAtGreaterThanEqualAndObservedAtLessThanEqualOrderByObservedAtAscIdAsc(
                serviceId, from, now)) {
      rows.add(
          new ServiceAccessSample(
              sample.getObservedAt(),
              sample.getElapsedMs(),
              sample.getStatus(),
              sample.getStatusRank(),
              sample.getObserver()));
    }
    return new ServiceAccessSamples(timeout.toMillis(), rows);
  }

  private static Instant truncateToTenMinutes(Instant instant) {
    long bucketSeconds = TEN_MINUTES * 60L;
    long epoch = instant.getEpochSecond();
    return Instant.ofEpochSecond(epoch - Math.floorMod(epoch, bucketSeconds));
  }

  private static List<ServiceAccessHour> hours(PaintedBucket[] row) {
    int count = row == null ? HOURS : row.length;
    List<ServiceAccessHour> hours = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      PaintedBucket bucket = row == null ? null : row[i];
      hours.add(bucket == null ? null : bucket.hour());
    }
    return hours;
  }

  private static final class PaintedBucket {
    private ServiceCheckSample winner;
    private final Set<String> observers = new TreeSet<>();

    void add(ServiceCheckSample sample) {
      observers.add(sample.getObserver());
      if (winner == null || worse(sample, winner)) {
        winner = sample;
      }
    }

    ServiceAccessHour hour() {
      return new ServiceAccessHour(
          winner.getStatus(), winner.getStatusRank(), List.copyOf(observers));
    }
  }

  private static boolean worse(ServiceCheckSample candidate, ServiceCheckSample current) {
    int rank = candidate.getStatusRank().compareTo(current.getStatusRank());
    if (rank != 0) {
      return rank > 0;
    }
    return candidate.getObservedAt().isAfter(current.getObservedAt());
  }

  private static ServiceCheck newer(List<ServiceCheck> rows) {
    return rows.stream()
        .max(
            Comparator.comparing(ServiceCheck::getObservedAt)
                .thenComparing(
                    ServiceCheck::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElseThrow();
  }
}
