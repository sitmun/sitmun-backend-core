package org.sitmun.infrastructure.security.config;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.LdapTemplate;

/**
 * Configuration for LDAP health indicator. Only enabled when the 'ldap' profile is active and LDAP
 * health checks are enabled.
 */
@Configuration
@Profile("ldap")
public class LdapHealthIndicatorConfig {

  /**
   * Configures the LDAP health indicator to be enabled only when: 1. The 'ldap' profile is active
   * 2. The management.health.ldap.enabled property is true
   */
  @Bean
  @ConditionalOnEnabledHealthIndicator("ldap")
  @ConditionalOnProperty(name = "management.health.ldap.enabled", havingValue = "true")
  public HealthIndicator ldapHealthIndicator(LdapTemplate ldapTemplate) {
    return new org.springframework.boot.actuate.ldap.LdapHealthIndicator(ldapTemplate);
  }
}
