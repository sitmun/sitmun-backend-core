package org.sitmun.plugin.core.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;

import java.text.MessageFormat;

public interface DashboardMetricsContributor extends Runnable {
  default MultiGauge.Row<Number> getNumberRow(Object[] entry) {
    if (entry[0] != null) {
      String key = MessageFormat.format("{0,number,0000}-{1,number,00}-{2,number,00}", entry[0], entry[1], entry[2]);
      Number value = (Number) entry[3];
      return MultiGauge.Row.of(Tags.of(DashboardInfoContributor.TAG, key), value);
    } else {
      return null;
    }
  }

}
