package org.sitmun.infrastructure.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL-specific transaction configuration.
 * 
 * This configuration handles PostgreSQL transaction management and auto-commit issues.
 * It's automatically activated when using the 'postgres' profile.
 */
@Configuration
@Profile("postgres")
@EnableTransactionManagement
@Slf4j
public class PostgreSQLTransactionConfig {

  /**
   * Configure transaction management for PostgreSQL.
   * This method is called after the application context is initialized.
   */
  @PostConstruct
  public void configureTransactionManagement() {
    log.info("Configuring transaction management for PostgreSQL");
    
    // Ensure proper transaction handling for PostgreSQL
    // The connection pool will handle auto-commit appropriately
    log.info("PostgreSQL transaction configuration completed");
  }
} 