package org.sitmun.administration.service.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import jakarta.persistence.EntityManager;

public class GenericDashboardMetricsContributor implements DashboardMetricsContributor {

  private final MultiGauge gauge;

  private final EntityManager entityManager;

  private final DashboardProperties.MetricDefinition definition;

  public GenericDashboardMetricsContributor(
      MultiGauge gauge,
      EntityManager entityManager,
      DashboardProperties.MetricDefinition definition) {
    this.gauge = gauge;
    this.entityManager = entityManager;
    this.definition = definition;
  }

  @Override
  public void run() {
    runQuery(gauge, entityManager, definition);
  }
}
