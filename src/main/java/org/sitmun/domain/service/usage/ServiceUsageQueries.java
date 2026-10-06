package org.sitmun.domain.service.usage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServiceUsageQueries {

  static final int DAYS_30 = 30;

  private final ServiceUsageRepository usage;
  private final ServiceRepository services;
  private final ApplicationRepository applications;
  private final boolean proxyForced;
  private final ZoneId zone;

  @Autowired
  public ServiceUsageQueries(
      ServiceUsageRepository usage,
      ServiceRepository services,
      ApplicationRepository applications,
      @Value("${sitmun.proxy-middleware.force}") boolean proxyForced) {
    this(usage, services, applications, proxyForced, ZoneId.systemDefault());
  }

  ServiceUsageQueries(
      ServiceUsageRepository usage,
      ServiceRepository services,
      ApplicationRepository applications,
      boolean proxyForced,
      ZoneId zone) {
    this.usage = usage;
    this.services = services;
    this.applications = applications;
    this.proxyForced = proxyForced;
    this.zone = zone;
  }

  public static LocalDate thirtyDayStart(LocalDate today) {
    return today.minusDays(DAYS_30 - 1L);
  }

  public static LocalDate thirtyDayEnd(LocalDate today) {
    return today.plusDays(1);
  }

  static boolean measured(Boolean proxied, boolean force) {
    return force || Boolean.TRUE.equals(proxied);
  }

  @Transactional(readOnly = true)
  public Map<Integer, UsageWindow> windows(
      Iterable<Integer> serviceIds, Map<Integer, Boolean> proxied, Instant now) {
    LocalDate today = now.atZone(zone).toLocalDate();
    LocalDate from = thirtyDayStart(today);
    Map<Integer, List<ServiceUsage>> rows = rowsByService(from, thirtyDayEnd(today));
    Map<Integer, UsageWindow> windows = new HashMap<>();
    for (Integer serviceId : serviceIds) {
      boolean isMeasured = measured(proxied.get(serviceId), proxyForced);
      windows.put(
          serviceId,
          summarize(isMeasured, rows.getOrDefault(serviceId, List.of()), from, DAYS_30, false)
              .window());
    }
    return windows;
  }

  @Transactional(readOnly = true)
  public ServiceUsageView view(Integer serviceId, String range, Instant now) {
    Service service =
        services
            .findById(serviceId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Service " + serviceId + " not found"));
    UsageSpan span = UsageSpan.parse(range);
    LocalDate today = now.atZone(zone).toLocalDate();
    LocalDate from = span.from(today);
    LocalDate to = span.to(today);
    List<ServiceUsage> rows =
        usage.findByIdServiceIdAndIdUsageDayGreaterThanEqualAndIdUsageDayLessThan(
            serviceId, from, to);
    boolean isMeasured = measured(service.getIsProxied(), proxyForced);
    Long previousTotal = null;
    Long previousFailed = null;
    if (isMeasured) {
      LocalDate previousFrom = span.previousFrom(today);
      Summary previous =
          summarize(
              true,
              usage.findByIdServiceIdAndIdUsageDayGreaterThanEqualAndIdUsageDayLessThan(
                  serviceId, previousFrom, from),
              previousFrom,
              span.points(),
              span.monthly());
      previousTotal = previous.total();
      previousFailed = previous.failed();
    }
    return summarize(isMeasured, rows, from, span.points(), span.monthly())
        .view(names(rows), previousTotal, previousFailed, now);
  }

  private Map<Integer, List<ServiceUsage>> rowsByService(LocalDate from, LocalDate to) {
    Map<Integer, List<ServiceUsage>> rows = new HashMap<>();
    for (ServiceUsage row : usage.findInWindow(from, to)) {
      rows.computeIfAbsent(row.getId().getServiceId(), id -> new ArrayList<>()).add(row);
    }
    return rows;
  }

  private Map<Integer, String> names(List<ServiceUsage> rows) {
    List<Integer> ids =
        rows.stream().map(row -> row.getId().getApplicationId()).distinct().toList();
    Map<Integer, String> names = new HashMap<>();
    if (ids.isEmpty()) {
      return names;
    }
    for (Application application : applications.findAllById(ids)) {
      names.put(application.getId(), application.getName());
    }
    return names;
  }

  private static Summary summarize(
      boolean measured, List<ServiceUsage> rows, LocalDate from, int points, boolean monthly) {
    long viewerLoads = 0;
    long total = 0;
    long failed = 0;
    LocalDate lastUsed = null;
    long[] buckets = new long[points];
    long[] failedBuckets = new long[points];
    Map<String, long[]> byOperation = new HashMap<>();
    Map<Integer, long[]> byApplication = new HashMap<>();
    for (ServiceUsage row : rows) {
      boolean request = UsageOperation.countsAsRequest(row.getId().getOperation());
      if (!request) {
        viewerLoads += row.getRequests();
      }
      int index =
          monthly
              ? monthIndex(from, row.getId().getUsageDay())
              : dayIndex(from, row.getId().getUsageDay());
      if (request && measured && index >= 0 && index < points) {
        buckets[index] += row.getRequests();
        failedBuckets[index] += row.getFailed();
        total += row.getRequests();
        failed += row.getFailed();
        if (row.getRequests() > 0
            && (lastUsed == null || row.getId().getUsageDay().isAfter(lastUsed))) {
          lastUsed = row.getId().getUsageDay();
        }
      }
      long[] operation =
          byOperation.computeIfAbsent(row.getId().getOperation(), key -> new long[3]);
      operation[0] += row.getRequests();
      operation[1] += row.getFailed();
      if (!request) {
        operation[2] += row.getRequests();
      }
      long[] application =
          byApplication.computeIfAbsent(row.getId().getApplicationId(), key -> new long[3]);
      if (request) {
        application[0] += row.getRequests();
        application[1] += row.getFailed();
      } else {
        application[2] += row.getRequests();
      }
    }
    return new Summary(
        measured,
        total,
        failed,
        lastUsed,
        viewerLoads,
        buckets,
        failedBuckets,
        from,
        monthly,
        byOperation,
        byApplication);
  }

  private static int dayIndex(LocalDate from, LocalDate day) {
    return (int) ChronoUnit.DAYS.between(from, day);
  }

  private static int monthIndex(LocalDate from, LocalDate day) {
    return (int) ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(day));
  }

  private record Summary(
      boolean measured,
      long total,
      long failed,
      LocalDate lastUsed,
      long viewerLoads,
      long[] buckets,
      long[] failedBuckets,
      LocalDate from,
      boolean monthly,
      Map<String, long[]> byOperation,
      Map<Integer, long[]> byApplication) {

    UsageWindow window() {
      if (!measured) {
        return new UsageWindow(false, null, null, null, viewerLoads, null);
      }
      return new UsageWindow(true, total, failed, lastUsed, viewerLoads, boxed(buckets));
    }

    ServiceUsageView view(
        Map<Integer, String> names, Long previousTotal, Long previousFailed, Instant asOf) {
      if (!measured) {
        return new ServiceUsageView(
            false,
            null,
            null,
            null,
            viewerLoads,
            null,
            operations(false),
            applications(names, false),
            null,
            null,
            asOf);
      }
      return new ServiceUsageView(
          true,
          total,
          failed,
          lastUsed,
          viewerLoads,
          series(),
          operations(true),
          applications(names, true),
          previousTotal,
          previousFailed,
          asOf);
    }

    private List<UsagePoint> series() {
      List<UsagePoint> points = new ArrayList<>(buckets.length);
      for (int i = 0; i < buckets.length; i++) {
        points.add(new UsagePoint(label(i), buckets[i], failedBuckets[i]));
      }
      return points;
    }

    private String label(int index) {
      if (monthly) {
        return YearMonth.from(from).plusMonths(index).toString();
      }
      return from.plusDays(index).toString();
    }

    private List<UsageOperationTotal> operations(boolean includeRequests) {
      List<UsageOperationTotal> totals = new ArrayList<>();
      byOperation.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(
              entry -> {
                boolean request = UsageOperation.countsAsRequest(entry.getKey());
                if (!includeRequests && request) {
                  return;
                }
                long[] counts = entry.getValue();
                totals.add(
                    new UsageOperationTotal(
                        entry.getKey(), request ? counts[0] : counts[2], request ? counts[1] : 0L));
              });
      return totals;
    }

    private List<UsageApplicationTotal> applications(Map<Integer, String> names, boolean requests) {
      return byApplication.entrySet().stream()
          .filter(entry -> requests ? entry.getValue()[0] > 0 : entry.getValue()[2] > 0)
          .sorted(
              Comparator.comparingLong((Map.Entry<Integer, long[]> entry) -> rank(entry, requests))
                  .reversed())
          .map(
              entry ->
                  new UsageApplicationTotal(
                      entry.getKey(),
                      names.getOrDefault(entry.getKey(), Integer.toString(entry.getKey())),
                      requests ? entry.getValue()[0] : null,
                      requests ? entry.getValue()[1] : null,
                      entry.getValue()[2]))
          .toList();
    }

    private static long rank(Map.Entry<Integer, long[]> entry, boolean requests) {
      return requests ? entry.getValue()[0] : entry.getValue()[2];
    }
  }

  private static List<Long> boxed(long[] values) {
    List<Long> boxed = new ArrayList<>(values.length);
    for (long value : values) {
      boxed.add(value);
    }
    return boxed;
  }

  private record UsageSpan(String raw) {
    static UsageSpan parse(String range) {
      if ("30d".equals(range) || "90d".equals(range) || "12m".equals(range)) {
        return new UsageSpan(range);
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range");
    }

    LocalDate previousFrom(LocalDate today) {
      return switch (raw) {
        case "30d" -> from(today).minusDays(DAYS_30);
        case "90d" -> from(today).minusDays(90);
        case "12m" -> from(today).minusMonths(12);
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range");
      };
    }

    LocalDate from(LocalDate today) {
      return switch (raw) {
        case "30d" -> thirtyDayStart(today);
        case "90d" -> today.minusDays(89);
        case "12m" -> today.withDayOfMonth(1).minusMonths(11);
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range");
      };
    }

    LocalDate to(LocalDate today) {
      if ("12m".equals(raw)) {
        return today.withDayOfMonth(1).plusMonths(1);
      }
      if ("30d".equals(raw)) {
        return thirtyDayEnd(today);
      }
      return today.plusDays(1);
    }

    int points() {
      return switch (raw) {
        case "30d" -> 30;
        case "90d" -> 90;
        case "12m" -> 12;
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range");
      };
    }

    boolean monthly() {
      return "12m".equals(raw);
    }
  }
}
