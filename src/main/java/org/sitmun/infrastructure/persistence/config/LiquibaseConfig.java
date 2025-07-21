package org.sitmun.infrastructure.persistence.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.persistence.liquibase.AsyncSpringLiquibase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableConfigurationProperties({LiquibaseProperties.class})
@Slf4j
public class LiquibaseConfig {

  private final Environment env;

  private final TaskExecutor taskExecutor;

  private final LiquibaseProperties liquibaseProperties;

  public LiquibaseConfig(
      LiquibaseProperties liquibaseProperties, TaskExecutor taskExecutor, Environment env) {
    this.liquibaseProperties = liquibaseProperties;
    this.taskExecutor = taskExecutor;
    this.env = env;
  }

  @Bean
  public SpringLiquibase liquibase(DataSource dataSource) {

    SpringLiquibase liquibase = new AsyncSpringLiquibase(taskExecutor, env);
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(liquibaseProperties.getChangeLog());
    liquibase.setContexts(liquibaseProperties.getContexts());
    liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
    liquibase.setDropFirst(liquibaseProperties.isDropFirst());
    liquibase.setShouldRun(liquibaseProperties.isEnabled());
    log.debug("Configuring Liquibase");
    return liquibase;
  }
}
