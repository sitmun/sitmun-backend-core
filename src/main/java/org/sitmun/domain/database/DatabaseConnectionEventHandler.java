package org.sitmun.domain.database;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class DatabaseConnectionEventHandler {

  @HandleBeforeCreate
  public void handleUserCreate(@NotNull DatabaseConnection databaseConnection) {
    if (databaseConnection.getPassword() != null && databaseConnection.getPassword().isEmpty()) {
      databaseConnection.setPassword(null);
    }
  }

  @HandleBeforeSave
  public void handleUserUpdate(@NotNull DatabaseConnection databaseConnection) {
    if (databaseConnection.getPassword() == null) {
      databaseConnection.setPassword(databaseConnection.getStoredPassword());
    } else if (databaseConnection.getPassword().isEmpty()) {
      databaseConnection.setPassword(null);
    }
  }
}
