package org.sitmun.infrastructure.security.config;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Configuration for mail health indicator. Only enabled when the 'mail' profile is active and mail
 * health checks are enabled.
 */
@Configuration
@Profile("mail")
public class MailHealthIndicatorConfig {

  /**
   * Configures the mail health indicator to be enabled only when: 1. The 'mail' profile is active
   * 2. The management.health.mail.enabled property is true
   */
  @Bean
  @ConditionalOnEnabledHealthIndicator("mail")
  @ConditionalOnProperty(name = "management.health.mail.enabled", havingValue = "true")
  public HealthIndicator mailHealthIndicator(JavaMailSender mailSender) {
    // Spring Boot 2.7 doesn't have a built-in MailHealthIndicator
    // We'll create a simple health indicator that checks if mail sender is available
    return () -> {
      try {
        // Simple check to see if mail sender is configured
        if (mailSender != null) {
          return org.springframework.boot.actuate.health.Health.up()
              .withDetail("mail", "Mail service is available")
              .build();
        } else {
          return org.springframework.boot.actuate.health.Health.down()
              .withDetail("mail", "Mail service is not available")
              .build();
        }
      } catch (Exception e) {
        return org.springframework.boot.actuate.health.Health.down()
            .withDetail("mail", "Mail service error: " + e.getMessage())
            .build();
      }
    };
  }
}
