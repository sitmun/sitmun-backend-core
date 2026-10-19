package org.sitmun.infrastructure.security.config;

import org.sitmun.infrastructure.config.Profiles;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.LdapTemplate;

/**
 * Configuration for LDAP health indicator. Only enabled when the 'ldap' profile is active and LDAP
 * health checks are enabled.
 */
@Configuration
@Profile(Profiles.LDAP)
public class LdapHealthIndicatorConfig {

  /**
   * Configures the LDAP health indicator to be enabled only when: 1. The 'ldap' profile is active
   * 2. The management.health.ldap.enabled property is true
   */
  @Bean
  @ConditionalOnBean(LdapTemplate.class)
  @ConditionalOnEnabledHealthIndicator("ldap")
  @ConditionalOnProperty(name = "management.health.ldap.enabled", havingValue = "true")
  public HealthIndicator ldapHealthIndicator(LdapTemplate ldapTemplate) {
    return new org.sitmun.infrastructure.security.health.CustomLdapHealthIndicator(ldapTemplate);
  }
}
