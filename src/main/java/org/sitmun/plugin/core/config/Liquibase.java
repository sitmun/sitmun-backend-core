package org.sitmun.plugin.core.config;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.sitmun.plugin.core.repository.handlers.SyncEntityHandler;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.List;

class Liquibase extends SpringLiquibase {

  private final List<SyncEntityHandler> syncEntityHandlers;

  private final TaskExecutor taskExecutor;

  public Liquibase(TaskExecutor taskExecutor, List<SyncEntityHandler> syncEntityHandlers) {
    super();
    this.taskExecutor = taskExecutor;
    this.syncEntityHandlers = syncEntityHandlers;
  }

  @Override
  public void afterPropertiesSet() {
    if (taskExecutor != null) {
      taskExecutor.execute(() -> {
        try {
          log.info("Starting Liquibase asynchronously, your database might not be ready at startup!");
          initDb();
        } catch (LiquibaseException e) {
          log.severe("Liquibase could not start correctly, your database is NOT ready: " + e.getMessage(), e);
        }
      });
    } else {
      try {
        log.info("Starting Liquibase!");
        initDb();
      } catch (LiquibaseException e) {
        log.severe("Liquibase could not start correctly, your database is NOT ready: " + e.getMessage(), e);
      }
    }
  }

  protected void initDb() throws LiquibaseException {
    StopWatch watch = new StopWatch();
    watch.start();
    super.afterPropertiesSet();
    SecurityContext sc = SecurityContextHolder.getContext();
    sc.setAuthentication(new AdminAuthentication());
    syncEntityHandlers.forEach(SyncEntityHandler::synchronize);
    SecurityContextHolder.clearContext();
    watch.stop();
    log.info("Started Liquibase in " + watch.getTotalTimeMillis() + "ms");
  }

  private static class AdminAuthentication implements Authentication {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return null;
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getDetails() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return "admin";
    }

    @Override
    public boolean isAuthenticated() {
      return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @Override
    public String getName() {
      return null;
    }
  }

}
