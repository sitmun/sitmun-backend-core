package org.sitmun.domain.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class ServiceEventHandler {

  static final String AUTH_NONE = "None";

  @HandleBeforeCreate
  public void handleBeforeCreate(@NotNull Service service) {
    if (service.getPassword() != null && service.getPassword().isEmpty()) {
      service.setPassword(null);
    }
    forceProxiedWhenAuthenticated(service);
  }

  @HandleBeforeSave
  public void handleBeforeSave(@NotNull Service service) {
    keepPassword(service);
    forceProxiedWhenAuthenticated(service);
  }

  private static void keepPassword(Service service) {
    if (service.getPassword() == null) {
      service.setPassword(service.getStoredPassword());
    } else if (service.getPassword().isEmpty()) {
      service.setPassword(null);
    }
  }

  private static void forceProxiedWhenAuthenticated(Service service) {
    String mode = service.getAuthenticationMode();
    if (mode != null && !mode.isBlank() && !AUTH_NONE.equals(mode)) {
      service.setIsProxied(true);
    }
  }
}
