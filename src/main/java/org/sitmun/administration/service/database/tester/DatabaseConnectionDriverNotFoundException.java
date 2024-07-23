package org.sitmun.administration.service.database.tester;

import org.sitmun.domain.database.DatabaseConnection;

/**
 * Thrown to indicate that a {@link DatabaseConnection#getDriver()} cannot be loaded.
 */
public class DatabaseConnectionDriverNotFoundException extends RuntimeException {
  public DatabaseConnectionDriverNotFoundException(Exception e) {
    super(e);
  }
}
