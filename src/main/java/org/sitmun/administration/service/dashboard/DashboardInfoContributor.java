package org.sitmun.administration.service.dashboard;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class DashboardInfoContributor implements InfoContributor {

  static final String TAG = "dashboard";

  private final MeterRegistry meterRegistry;

  public DashboardInfoContributor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  private static Pair<String, Double> processMetric(Meter meter) {
    if (meter instanceof Gauge gauge) {
      String id = gauge.getId().getName().substring(DashboardConfig.METRICS_PREFIX.length());
      String suffix = gauge.getId().getTag(TAG);
      if (suffix != null) {
        id = id + '.' + suffix;
      }
      return Pair.of(id, gauge.value());
    }
    return null;
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail(TAG, hierarchiseMetrics(collectMetrics()));
  }

  private Map<String, Object> hierarchiseMetrics(Map<String, Object> map) {
    return map.entrySet().stream()
        .map(entry -> Pair.of(entry.getKey().split("\\."), entry.getValue()))
        .collect(groupingBy(entry -> entry.getKey()[0]))
        .entrySet()
        .stream()
        .map(this::processEntry)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Object> collectMetrics() {
    return Search.in(meterRegistry)
        .name(name -> name.startsWith(DashboardConfig.METRICS_PREFIX))
        .meters()
        .stream()
        .map(DashboardInfoContributor::processMetric)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map.Entry<String, Object> processEntry(
      Map.Entry<String, List<Pair<String[], Object>>> entry) {
    Object newValue =
        toValue(
            entry.getValue().stream()
                .map(
                    value ->
                        Pair.of(
                            Arrays.stream(value.getKey()).skip(1).toArray(String[]::new),
                            value.getValue()))
                .collect(toList()));
    if (newValue != null) {
      return new AbstractMap.SimpleEntry<>(entry.getKey(), newValue);
    }
    return null;
  }

  private Object toValue(List<Pair<String[], Object>> list) {
    if (list.isEmpty()) {
      return null;
    }
    if (list.size() == 1 && list.get(0).getKey().length == 0) {
      return list.get(0).getValue();
    }
    return list.stream()
        .filter(entry -> entry.getKey().length != 0)
        .collect(groupingBy(entry -> entry.getKey()[0]))
        .entrySet()
        .stream()
        .map(this::processEntry)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
