package org.sitmun.plugin.core.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import org.sitmun.plugin.core.properties.DashboardProperties.MetricDefinition;
import org.sitmun.plugin.core.repository.CartographyRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class CartographiesByCreatedDateSinceDate implements DashboardMetricsContributor {

  private final MultiGauge gauge;

  private final CartographyRepository cartographyRepository;

  private final MetricDefinition definition;

  public CartographiesByCreatedDateSinceDate(MultiGauge gauge, CartographyRepository cartographyRepository, MetricDefinition definition) {
    this.gauge = gauge;
    this.cartographyRepository = cartographyRepository;
    this.definition = definition;
  }

  @Override
  public void run() {
    if (definition.getSize() > 0) {
      gauge.register(
        StreamSupport
          .stream(cartographyRepository.cartographiesByCreatedDateSinceDate(Date.valueOf(LocalDate.now().minusDays(definition.getSize()))).spliterator(), false)
          .map(this::getDateNumberRow)
          .filter(Objects::nonNull)
          .collect(toList())
      );
    }
  }
}
