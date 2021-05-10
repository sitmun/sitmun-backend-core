package org.sitmun.plugin.core.repository.handlers;

import org.sitmun.plugin.core.domain.DatabaseConnection;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
@RepositoryEventHandler
public class DatabaseConnectionEventHandler {

  /**
   * If the password is null or empty, this method sets the password null.
   *
   * @param databaseConnection the new database connection
   */
  @HandleBeforeCreate
  public void handleUserCreate(@NotNull DatabaseConnection databaseConnection) {
    if (databaseConnection.getPassword() != null) {
      if (databaseConnection.getPassword().isEmpty()) {
        databaseConnection.setPassword(null);
      }
    }
  }

  /**
   * If the password is null, this method keeps the last value if exists,
   * if the password is empty, this method clears it,
   *
   * @param databaseConnection the new database connection after being loaded from database and updated with PUT data
   */
  @HandleBeforeSave
  public void handleUserUpdate(@NotNull DatabaseConnection databaseConnection) {
    if (databaseConnection.getPassword() == null) {
      databaseConnection.setPassword(databaseConnection.getStoredPassword());
    } else if (databaseConnection.getPassword().isEmpty()) {
      databaseConnection.setPassword(null);
    }
  }
}