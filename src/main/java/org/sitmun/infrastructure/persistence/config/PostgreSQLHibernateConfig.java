package org.sitmun.infrastructure.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL-specific Hibernate configuration.
 * 
 * This configuration handles PostgreSQL-specific issues like CLOB support.
 * It's automatically activated when using the 'postgres' profile.
 */
@Configuration
@Profile("postgres")
@Slf4j
public class PostgreSQLHibernateConfig {

  /**
   * Configure Hibernate properties for PostgreSQL.
   * This method is called after the application context is initialized.
   */
  @PostConstruct
  public void configureHibernateForPostgreSQL() {
    log.info("Configuring Hibernate for PostgreSQL");
    
    // Set system properties to disable CLOB creation
    System.setProperty("hibernate.jdbc.lob.non_contextual_creation", "true");
    
    log.info("Hibernate PostgreSQL configuration completed");
  }
} 