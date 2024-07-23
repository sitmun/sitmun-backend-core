package org.sitmun.administration.service.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import org.sitmun.domain.user.UserRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class UserPerApplicationSinceDate implements DashboardMetricsContributor {


  private final MultiGauge gauge;

  private final UserRepository userRepository;

  private final DashboardProperties.MetricDefinition definition;

  public UserPerApplicationSinceDate(MultiGauge gauge, UserRepository userRepository, DashboardProperties.MetricDefinition definition) {
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
          .map(this::processIdLabelValue)
          .filter(Objects::nonNull)
          .collect(toList())
      );
    }
  }
}
