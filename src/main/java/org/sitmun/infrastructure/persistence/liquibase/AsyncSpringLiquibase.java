package org.sitmun.infrastructure.persistence.liquibase;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StopWatch;

@Slf4j
public class AsyncSpringLiquibase extends SpringLiquibase {

  private final TaskExecutor taskExecutor;
  private final Environment env;

  public AsyncSpringLiquibase(TaskExecutor taskExecutor, Environment env) {
    this.taskExecutor = taskExecutor;
    this.env = env;
  }

  @Override
  public void afterPropertiesSet()  {
    if (env.acceptsProfiles(Constants.heroku)) {
      taskExecutor.execute(() -> {
        try {
          log.warn("Starting Liquibase asynchronously, your database might not be ready at startup!");
          initDb();
        } catch (LiquibaseException e) {
          log.error("Liquibase could not start correctly, your database is NOT ready: {}", e.getMessage(), e);
        }
      });
    } else {
      try {
        log.warn("Starting Liquibase synchronously");
        initDb();
      } catch (LiquibaseException e) {
        log.error("Liquibase could not start correctly, your database is NOT ready: {}", e.getMessage(), e);
      }
    }
  }

  protected void initDb() throws LiquibaseException {
    StopWatch watch = new StopWatch();
    watch.start();
    super.afterPropertiesSet();
    watch.stop();
    log.warn("Started Liquibase in {} ms", watch.getTotalTimeMillis());
  }

}
