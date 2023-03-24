package org.sitmun.infrastructure.persistence.liquibase;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Liquibase extends SpringLiquibase {

  private final List<SyncEntityHandler> syncEntityHandlers;

  private final List<OnStartupUpdateHandler> onStartupUpdateHandlers;

  private final TaskExecutor taskExecutor;

  private boolean runAsync = false;

  private boolean syncLegacy = false;

  private boolean onStartupUpdate = true;

  public Liquibase(TaskExecutor taskExecutor,
                   List<SyncEntityHandler> syncEntityHandlers,
                   List<OnStartupUpdateHandler> onStartupUpdateHandlers) {
    super();
    this.taskExecutor = taskExecutor;
    this.syncEntityHandlers = syncEntityHandlers;
    this.onStartupUpdateHandlers = onStartupUpdateHandlers;
  }

  @Override
  public void afterPropertiesSet() {
    if (runAsync) {
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
    if (syncLegacy) {
      SecurityContext sc = SecurityContextHolder.getContext();
      sc.setAuthentication(new AdminAuthentication());
      syncEntityHandlers.forEach(SyncEntityHandler::synchronize);
      SecurityContextHolder.clearContext();
    }
    if (onStartupUpdate) {
      SecurityContext sc = SecurityContextHolder.getContext();
      sc.setAuthentication(new AdminAuthentication());
      onStartupUpdateHandlers.forEach(OnStartupUpdateHandler::update);
      SecurityContextHolder.clearContext();
    }
    watch.stop();
    log.info("Started Liquibase in " + watch.getTotalTimeMillis() + "ms");
  }

  public Boolean getOnStartupUpdate() {
    return onStartupUpdate;
  }

  public void setOnStartupUpdate(Boolean onStartupUpdate) {
    this.onStartupUpdate = onStartupUpdate;
  }

  public Boolean getRunAsync() {
    return runAsync;
  }

  public void setRunAsync(Boolean runAsync) {
    this.runAsync = runAsync;
  }

  public Boolean getSyncLegacy() {
    return syncLegacy;
  }

  public void setSyncLegacy(Boolean syncLegacy) {
    this.syncLegacy = syncLegacy;
  }

  private static class AdminAuthentication implements Authentication {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return Collections.emptyList();
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
      // Dummy operation
    }

    @Override
    public String getName() {
      return null;
    }
  }

}
