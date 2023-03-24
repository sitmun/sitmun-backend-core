package org.sitmun.administration.service.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;

public interface DashboardMetricsContributor extends Runnable {
  default MultiGauge.Row<Number> getDateNumberRow(Object[] entry) {
    if (entry == null) {
      return null;
    } else if (entry.length == 4 && Arrays.stream(entry).allMatch(Objects::nonNull)) {
      String key = MessageFormat.format("{0,number,0000}-{1,number,00}-{2,number,00}", entry[0], entry[1], entry[2]);
      Number value = (Number) entry[3];
      return MultiGauge.Row.of(Tags.of(DashboardInfoContributor.TAG, key), value);
    } else if (entry.length == 3 && Arrays.stream(entry).allMatch(Objects::nonNull)) {
      String key = MessageFormat.format("{0,number,0000}-{1,number,00}", entry[0], entry[1]);
      Number value = (Number) entry[2];
      return MultiGauge.Row.of(Tags.of(DashboardInfoContributor.TAG, key), value);
    } else {
      return null;
    }
  }

  default MultiGauge.Row<Number> getStringNumberRow(Object[] entry) {
    if (entry != null && entry.length == 2 && Arrays.stream(entry).allMatch(Objects::nonNull)) {
      String key = (String) entry[0];
      Number value = (Number) entry[1];
      return MultiGauge.Row.of(Tags.of(DashboardInfoContributor.TAG, key), value);
    } else {
      return null;
    }
  }
}
