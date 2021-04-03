package org.sitmun.plugin.core.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import org.sitmun.plugin.core.config.MetricsProperties.MetricDefinition;
import org.sitmun.plugin.core.repository.UserConfigurationRepository;
import org.springframework.data.domain.PageRequest;

import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class UserConfigurationsByCreatedDate implements DashboardMetricsContributor {


  private final MultiGauge gauge;

  private final UserConfigurationRepository userRepository;

  private final MetricDefinition definition;

  public UserConfigurationsByCreatedDate(MultiGauge gauge, UserConfigurationRepository userRepository, MetricDefinition definition) {
    this.gauge = gauge;
    this.userRepository = userRepository;
    this.definition = definition;
  }

  @Override
  public void run() {
    if (definition.getSize() > 0) {
      gauge.register(
        userRepository.userConfigurationsByCreatedDate(PageRequest.of(0, definition.getSize())).stream()
          .map(this::getNumberRow)
          .filter(Objects::nonNull)
          .collect(toList())
      );
    }
  }
}
