package org.sitmun.feature.admin.dashboard;

import com.google.common.collect.Maps;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Component
public class DashboardInfoContributor implements InfoContributor {

  static final String TAG = "dashboard";

  private final MeterRegistry meterRegistry;

  public DashboardInfoContributor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail("dashboard", hierarchiseMetrics(collectMetrics()));
  }

  private Map<String, Object> collectMetrics() {
    return Search.in(meterRegistry)
      .name(name -> name.startsWith(DashboardConfig.METRICS_PREFIX))
      .meters().stream()
      .map(this::processMetric)
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Object> hierarchiseMetrics(Map<String, Object> map) {
    return map.entrySet().stream()
      .map(entry -> Maps.immutableEntry(entry.getKey().split("\\."), entry.getValue()))
      .collect(groupingBy(entry -> entry.getKey()[0]))
      .entrySet().stream()
      .map(this::processEntry)
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map.Entry<String, Object> processEntry(Map.Entry<String, List<Map.Entry<String[], Object>>> entry) {
    Object newValue = toValue(entry.getValue().stream()
      .map(value -> Maps.immutableEntry(
        Arrays.stream(value.getKey()).skip(1).toArray(String[]::new),
        value.getValue())
      ).collect(toList()));
    if (newValue != null) {
      return Maps.immutableEntry(entry.getKey(), newValue);
    } else return null;
  }

  private Object toValue(List<Map.Entry<String[], Object>> list) {
    if (list.size() == 0) {
      return null;
    } else if (list.size() == 1 && list.get(0).getKey().length == 0) {
      return list.get(0).getValue();
    } else {
      return list.stream()
        .filter(entry -> entry.getKey().length != 0)
        .collect(groupingBy(entry -> entry.getKey()[0]))
        .entrySet().stream()
        .map(this::processEntry)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
  }

  private Map.Entry<String, Double> processMetric(Meter meter) {
    if (meter instanceof Gauge) {
      Gauge gauge = (Gauge) meter;
      String id = gauge.getId().getName().substring(DashboardConfig.METRICS_PREFIX.length());
      String suffix = gauge.getId().getTag(TAG);
      if (suffix != null) {
        id = id + "." + suffix;
      }
      return Maps.immutableEntry(id, gauge.value());
    } else {
      return null;
    }
  }
}
