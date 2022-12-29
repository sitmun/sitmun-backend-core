package org.sitmun.feature.admin.dashboard;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sitmun.dashboard")
@Setter
@Getter
public class DashboardProperties {

  private MetricDefinition cartographiesByCreatedDate = new MetricDefinition();

  private MetricDefinition usersByCreatedDate = new MetricDefinition();

  private MetricDefinition usersPerApplication = new MetricDefinition();

  @Setter
  @Getter
  public static class MetricDefinition {

    private Integer size = 0;

  }
}
