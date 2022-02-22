package org.sitmun.config;

import liquibase.integration.spring.SpringLiquibase;
import org.sitmun.repository.handlers.OnStartupUpdateHandler;
import org.sitmun.repository.handlers.SyncEntityHandler;
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
