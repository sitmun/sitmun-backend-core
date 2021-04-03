package org.sitmun.plugin.core.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import org.sitmun.plugin.core.config.MetricsProperties.MetricDefinition;
import org.sitmun.plugin.core.repository.CartographyRepository;
import org.springframework.data.domain.PageRequest;

import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class CartographiesByCreatedDate implements DashboardMetricsContributor {

  private final MultiGauge gauge;

  private final CartographyRepository cartographyRepository;

  private final MetricDefinition definition;

  public CartographiesByCreatedDate(MultiGauge gauge, CartographyRepository cartographyRepository, MetricDefinition definition) {
    this.gauge = gauge;
    this.cartographyRepository = cartographyRepository;
    this.definition = definition;
  }

  @Override
  public void run() {
    if (definition.getSize() > 0) {
      gauge.register(
        cartographyRepository.cartographiesByCreatedDate(PageRequest.of(0, definition.getSize())).stream()
          .map(this::getNumberRow)
          .filter(Objects::nonNull)
          .collect(toList())
      );
    }
  }
}
