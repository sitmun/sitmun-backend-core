package org.sitmun.plugin.core.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import org.sitmun.plugin.core.properties.DashboardProperties.MetricDefinition;
import org.sitmun.plugin.core.repository.UserRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class UserPerApplicationSinceDate implements DashboardMetricsContributor {


  private final MultiGauge gauge;

  private final UserRepository userRepository;

  private final MetricDefinition definition;

  public UserPerApplicationSinceDate(MultiGauge gauge, UserRepository userRepository, MetricDefinition definition) {
    this.gauge = gauge;
    this.userRepository = userRepository;
    this.definition = definition;
  }

  @Override
  public void run() {
    if (definition.getSize() > 0) {
      gauge.register(
        StreamSupport
          .stream(userRepository.usersPerApplicationSinceDate(Date.valueOf(LocalDate.now().minusDays(definition.getSize()))).spliterator(), false)
          .map(this::getStringNumberRow)
          .filter(Objects::nonNull)
          .collect(toList())
      );
    }
  }
}
