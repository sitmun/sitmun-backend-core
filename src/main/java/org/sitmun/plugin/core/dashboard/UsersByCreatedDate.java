package org.sitmun.plugin.core.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import org.sitmun.plugin.core.config.MetricsProperties.MetricDefinition;
import org.sitmun.plugin.core.repository.UserRepository;
import org.springframework.data.domain.PageRequest;

import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class UsersByCreatedDate implements DashboardMetricsContributor {


  private final MultiGauge gauge;

  private final UserRepository userRepository;

  private final MetricDefinition definition;

  public UsersByCreatedDate(MultiGauge gauge, UserRepository userRepository, MetricDefinition definition) {
    this.gauge = gauge;
    this.userRepository = userRepository;
    this.definition = definition;
  }

  @Override
  public void run() {
    if (definition.getSize() > 0) {
      gauge.register(
        userRepository.usersByCreatedDate(PageRequest.of(0, definition.getSize())).stream()
          .map(this::getNumberRow)
          .filter(Objects::nonNull)
          .collect(toList())
      );
    }
  }
}
