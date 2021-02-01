package org.sitmun.plugin.core.web.exceptions;

import org.sitmun.plugin.core.domain.DatabaseConnection;

import java.sql.SQLException;

/**
 * Thrown to indicate that a {@link DatabaseConnection} has thrown a SQL exception.
 */
public class DatabaseSQLException extends RuntimeException {
  public DatabaseSQLException(SQLException exception) {
    super(exception);
  }
}
