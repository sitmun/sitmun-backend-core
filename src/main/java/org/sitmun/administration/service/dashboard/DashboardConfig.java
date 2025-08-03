package org.sitmun.administration.service.dashboard;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.persistence.EntityManager;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class DashboardConfig {

  public static final String METRICS_PREFIX = "sitmun.";

  @Bean
  public DashboardMetricsContributor cartographiesCreatedByDate(
      MeterRegistry registry, EntityManager entityManager, DashboardProperties metricsProperties) {
    MultiGauge gauge =
        MultiGauge.builder(METRICS_PREFIX + "cartographies-created-on-date").register(registry);
    return new GenericDashboardMetricsContributor(
        gauge, entityManager, metricsProperties.getCartographiesByCreatedDate());
  }

  @Bean
  public DashboardMetricsContributor usersCreatedByDate(
      MeterRegistry registry, EntityManager entityManager, DashboardProperties metricsProperties) {
    MultiGauge gauge =
        MultiGauge.builder(METRICS_PREFIX + "users-created-on-date").register(registry);
    return new GenericDashboardMetricsContributor(
        gauge, entityManager, metricsProperties.getUsersByCreatedDate());
  }

  @Bean
  public DashboardMetricsContributor usersPerApplication(
      MeterRegistry registry, EntityManager entityManager, DashboardProperties metricsProperties) {
    MultiGauge gauge =
        MultiGauge.builder(METRICS_PREFIX + "users-per-application").register(registry);
    return new GenericDashboardMetricsContributor(
        gauge, entityManager, metricsProperties.getUsersPerApplication());
  }

  @Bean
  public MeterBinder totalApplicationsGauge(ApplicationRepository applicationRepository) {
    return registry ->
        Gauge.builder(METRICS_PREFIX + "total.applications", applicationRepository::count)
            .description("Total of applications")
            .register(registry);
  }

  @Bean
  public MeterBinder totalUsersMetric(UserRepository usersRepository) {
    return registry ->
        Gauge.builder(METRICS_PREFIX + "total.users", usersRepository::count)
            .description("Total of users")
            .register(registry);
  }

  @Bean
  public MeterBinder totalTerritoriesMetric(TerritoryRepository territoryRepository) {
    return registry ->
        Gauge.builder(METRICS_PREFIX + "total.territories", territoryRepository::count)
            .description("Total of territories")
            .register(registry);
  }

  @Bean
  public MeterBinder totalCartographyMetric(CartographyRepository cartographyRepository) {
    return registry ->
        Gauge.builder(METRICS_PREFIX + "total.cartographies", cartographyRepository::count)
            .description("Total of cartographies")
            .register(registry);
  }

  @Bean
  public MeterBinder totalTasksMetric(TaskRepository taskRepository) {
    return registry ->
        Gauge.builder(METRICS_PREFIX + "total.tasks", taskRepository::count)
            .description("Total of tasks")
            .register(registry);
  }

  @Bean
  public MeterBinder totalServicesMetric(ServiceRepository serviceRepository) {
    return registry ->
        Gauge.builder(METRICS_PREFIX + "total.services", serviceRepository::count)
            .description("Total of services")
            .register(registry);
  }

  /**
   * select distinct count(uc.territory, application) from user_configuration, roles where
   * user_configuration.role in application.roles
   *
   * <p>SELECT DISTINCT COUNT(uc.territory, app) FROM userConfiguration uc, applications app WHERE
   * uc.role in app.roles
   */
  @Bean
  public MeterBinder sumApplicationTerritoriesMetric(ApplicationRepository applicationRepository) {
    return registry ->
        Gauge.builder(
                METRICS_PREFIX + "total.applications-territories",
                () -> applicationRepository.listIdApplicationsPerTerritories().size())
            .description("Sum of applications available in territories")
            .register(registry);
  }
}
