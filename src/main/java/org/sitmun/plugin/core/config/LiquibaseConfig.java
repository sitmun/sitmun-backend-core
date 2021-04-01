package org.sitmun.plugin.core.config;

import liquibase.integration.spring.SpringLiquibase;
import org.sitmun.plugin.core.repository.handlers.SyncEntityHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@Configuration
@Profile("!heroku")
public class LiquibaseConfig {

  @Value("${spring.liquibase.changelog}")
  private String changeLog;

  @Value("${sitmun.tasks.rebuild-properties-on-startup}")
  private Boolean rebuildOnStartup;

  @Bean
  public SpringLiquibase liquibase(DataSource datasource, List<SyncEntityHandler> syncEntityHandlers) {
    SpringLiquibase liquibase = new Liquibase(null, rebuildOnStartup ? syncEntityHandlers : Collections.emptyList());
    liquibase.setChangeLog(changeLog);
    liquibase.setDataSource(datasource);
    return liquibase;
  }
}
