package org.sitmun.administration.service.access;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.domain.service.usage.ServiceUsageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ServiceCheckDispatcher {

  private static final Logger log = LoggerFactory.getLogger(ServiceCheckDispatcher.class);

  private final ServiceCheckProperties properties;
  private final ServiceCheckInFlight inFlight;
  private final ServiceAccessCheckExecutor checks;
  private final ServiceCheckDueQuery due;
  private final Clock clock;
  private final Executor workers;
  private final ServiceUsageStore usage;
  private final ServiceCheckStore store;
  private final ProbeFlight flight = new ProbeFlight();

  public ServiceCheckDispatcher(
      ServiceCheckProperties properties,
      ServiceCheckInFlight inFlight,
      ServiceAccessCheckExecutor checks,
      ServiceCheckDueQuery due,
      Clock clock,
      @Qualifier("serviceCheckExecutor") Executor workers,
      ServiceUsageStore usage,
      ServiceCheckStore store) {
    this.properties = properties;
    this.inFlight = inFlight;
    this.checks = checks;
    this.due = due;
    this.clock = clock;
    this.workers = workers;
    this.usage = usage;
    this.store = store;
  }

  @Scheduled(
      fixedRateString = "${sitmun.service-check.dispatch-interval}",
      initialDelayString = "${sitmun.service-check.dispatch-interval}")
  public void dispatch() {
    if (!Boolean.TRUE.equals(properties.enabled())) {
      return;
    }
    if (!inFlight.tryAcquire()) {
      return;
    }
    Claimed claimed;
    try {
      claimed = claim(clock.instant().minus(properties.interval()));
    } catch (RuntimeException ex) {
      inFlight.release();
      throw ex;
    }
    if (claimed == null) {
      inFlight.release();
      return;
    }
    try {
      workers.execute(() -> run(claimed));
    } catch (RuntimeException ex) {
      release(claimed);
      inFlight.release();
      throw ex;
    }
  }

  @Scheduled(
      fixedRateString = "${sitmun.service-check.retention-interval}",
      initialDelayString = "${sitmun.service-check.retention-interval}")
  public void retain() {
    Instant now = clock.instant();
    store.deleteSamplesOlderThan(now.minus(properties.sampleRetention()));
    // Usage days are written in the JVM zone by the proxy flush and the viewer counters.
    usage.deleteExpired(now, properties.usageRetention(), ZoneId.systemDefault());
    warnIfOverdue(now);
  }

  private void run(Claimed claimed) {
    try {
      checks.check(claimed.id(), false);
    } catch (RuntimeException ex) {
      log.warn(
          "Service check probe failed for service {}: {}",
          claimed.id(),
          ex.getClass().getSimpleName());
    } finally {
      release(claimed);
      inFlight.release();
    }
  }

  private Claimed claim(Instant dueBefore) {
    for (; ; ) {
      Optional<DueService> selected = due.first(dueBefore, this::eligible);
      if (selected.isEmpty()) {
        return null;
      }
      DueService chosen = selected.get();
      String host = host(chosen.serviceUrl());
      synchronized (flight) {
        if (!flight.accepts(chosen.id(), host, properties.maxPerHost())) {
          continue;
        }
        flight.claim(chosen.id(), host);
        return new Claimed(chosen.id(), host);
      }
    }
  }

  private boolean eligible(DueService candidate) {
    String host = host(candidate.serviceUrl());
    synchronized (flight) {
      return flight.accepts(candidate.id(), host, properties.maxPerHost());
    }
  }

  private void release(Claimed claimed) {
    synchronized (flight) {
      flight.release(claimed.id(), claimed.host());
    }
  }

  private void warnIfOverdue(Instant now) {
    Instant oldest = store.oldestDueAt(now.minus(properties.interval()));
    if (oldest == null) {
      return;
    }
    Duration age = Duration.between(oldest, now);
    Duration limit = properties.interval().multipliedBy(properties.overdueWarnFactor());
    if (age.compareTo(limit) < 0) {
      return;
    }
    log.warn(
        "Oldest due service check is {} old, at or past {} times the probe interval",
        age,
        properties.overdueWarnFactor());
  }

  static String host(String serviceUrl) {
    if (serviceUrl == null || serviceUrl.isBlank()) {
      return "";
    }
    try {
      String parsed = URI.create(serviceUrl.trim()).getHost();
      if (parsed != null && !parsed.isBlank()) {
        return parsed.toLowerCase(Locale.ROOT);
      }
    } catch (IllegalArgumentException ex) {
      return serviceUrl;
    }
    return serviceUrl;
  }

  private record Claimed(Integer id, String host) {}

  private static final class ProbeFlight {
    private final Set<Integer> ids = new HashSet<>();
    private final Map<String, Integer> hosts = new HashMap<>();

    boolean accepts(Integer id, String host, int maxPerHost) {
      return !ids.contains(id) && hosts.getOrDefault(host, 0) < maxPerHost;
    }

    void claim(Integer id, String host) {
      ids.add(id);
      hosts.merge(host, 1, Integer::sum);
    }

    void release(Integer id, String host) {
      ids.remove(id);
      hosts.compute(host, (key, count) -> count == null || count <= 1 ? null : count - 1);
    }
  }
}
