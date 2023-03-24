package org.sitmun.administration.service.database.tester;

import org.sitmun.domain.database.DatabaseConnection;

import java.sql.SQLException;

/**
 * Thrown to indicate that a {@link DatabaseConnection} has thrown a SQL exception.
 */
public class DatabaseSQLException extends RuntimeException {
  public DatabaseSQLException(SQLException exception) {
    super(exception);
  }
}
