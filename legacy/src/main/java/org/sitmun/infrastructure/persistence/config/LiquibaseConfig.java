package org.sitmun.infrastructure.persistence.config;

import liquibase.integration.spring.SpringLiquibase;
import org.sitmun.infrastructure.persistence.liquibase.Liquibase;
import org.sitmun.infrastructure.persistence.liquibase.OnStartupUpdateHandler;
import org.sitmun.infrastructure.persistence.liquibase.SyncEntityHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class LiquibaseConfig {

  @Bean
  @ConfigurationProperties("spring.liquibase")
  public SpringLiquibase liquibase(DataSource datasource,
                                   TaskExecutor taskExecutor,
                                   List<SyncEntityHandler> syncEntityHandlers,
                                   List<OnStartupUpdateHandler> onStartupUpdateHandlers) {
    SpringLiquibase liquibase = new Liquibase(taskExecutor, syncEntityHandlers, onStartupUpdateHandlers);
    liquibase.setDataSource(datasource);
    return liquibase;
  }
}
