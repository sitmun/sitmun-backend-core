package org.sitmun.web.exceptions;

import org.sitmun.common.domain.database.DatabaseConnection;

/**
 * Thrown to indicate that a {@link DatabaseConnection#getDriver()} cannot be loaded.
 */
public class DatabaseConnectionDriverNotFoundException extends RuntimeException {
  public DatabaseConnectionDriverNotFoundException(Exception e) {
    super(e);
  }
}
