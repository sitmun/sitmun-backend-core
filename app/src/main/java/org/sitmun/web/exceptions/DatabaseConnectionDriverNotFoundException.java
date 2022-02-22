package org.sitmun.web.exceptions;

import org.sitmun.domain.DatabaseConnection;

/**
 * Thrown to indicate that a {@link DatabaseConnection#getDriver()} cannot be loaded.
 */
public class DatabaseConnectionDriverNotFoundException extends RuntimeException {
  public DatabaseConnectionDriverNotFoundException(Exception e) {
    super(e);
  }
}
