package org.sitmun.administration.service.dashboard;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

  private final List<DashboardMetricsContributor> contributors;

  public ScheduledTasks(List<DashboardMetricsContributor> contributors) {
    this.contributors = contributors;
  }

  @Scheduled(
      fixedRateString = "${sitmun.dashboard.fetchMetrics:60000}",
      initialDelayString = "${sitmun.dashboard.initialDelay:1000}")
  public void updateMetrics() {
    contributors.forEach(DashboardMetricsContributor::run);
  }
}
