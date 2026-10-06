package org.sitmun.administration.service.access;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.sitmun.upstream.http.UpstreamHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ServiceCheckProperties.class)
public class ServiceCheckConfiguration {

  @Bean
  ServiceCheckInFlight serviceCheckInFlight(ServiceCheckProperties properties) {
    return new ServiceCheckInFlight(properties.maxInFlight());
  }

  @Bean
  Clock serviceCheckClock() {
    return Clock.systemUTC();
  }

  @Bean
  UpstreamHttpClient serviceCheckHttpClient(
      ServiceCheckProperties properties,
      @Value("${sitmun.client.unsafe-allowed-hosts:*}") List<String> unsafeAllowedHosts) {
    return new UpstreamHttpClient(
        unsafeAllowedHosts, properties.connectTimeout(), properties.readTimeout(), List.of());
  }

  @Bean
  TimeLimiter serviceCheckTimeLimiter(ServiceCheckProperties properties) {
    return TimeLimiter.of(
        TimeLimiterConfig.custom()
            .timeoutDuration(properties.timeout())
            .cancelRunningFuture(true)
            .build());
  }

  @Bean(destroyMethod = "shutdown")
  ExecutorService serviceCheckExecutor() {
    return Executors.newCachedThreadPool(
        runnable -> {
          Thread thread = new Thread(runnable, "service-check");
          thread.setDaemon(true);
          return thread;
        });
  }
}
