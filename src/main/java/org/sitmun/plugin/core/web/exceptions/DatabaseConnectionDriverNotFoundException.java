package org.sitmun.plugin.core.web.exceptions;

import org.sitmun.plugin.core.domain.DatabaseConnection;

/**
 * Thrown to indicate that a {@link DatabaseConnection#getDriver()} cannot be loaded.
 */
public class DatabaseConnectionDriverNotFoundException extends RuntimeException {
  public DatabaseConnectionDriverNotFoundException(ReflectiveOperationException e) {
    super(e);
  }
}
