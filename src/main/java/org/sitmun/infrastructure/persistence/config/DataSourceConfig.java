package org.sitmun.infrastructure.persistence.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Custom DataSource configuration that disables auto-commit before the pool starts.
 *
 * <p>PostgreSQL requires {@code auto-commit=false} so that transactions remain open during result
 * set iteration. Without this, lazy collection loading (triggered by Spring Data REST projections)
 * causes "ResultSet is closed" errors because PostgreSQL closes cursors when statements
 * auto-commit.
 *
 * <p>This cannot be set via {@code spring.datasource.hikari.auto-commit} in YAML or environment
 * variables because the main DataSource is initialized early (even with a separate Liquibase
 * DataSource), sealing the HikariCP configuration before Spring can bind the property.
 *
 * <p>Requires {@code hibernate.connection.provider_disables_autocommit=true} in JPA properties so
 * Hibernate skips redundant auto-commit checks.
 */
@Configuration
public class DataSourceConfig {

  private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource.hikari")
  public HikariDataSource dataSource(DataSourceProperties properties) {
    HikariDataSource ds =
        properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    ds.setAutoCommit(false);
    log.info("DataSource configured with auto-commit={}", ds.isAutoCommit());
    return ds;
  }
}
