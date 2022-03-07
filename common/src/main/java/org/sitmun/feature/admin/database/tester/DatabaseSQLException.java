package org.sitmun.feature.admin.database.tester;

import org.sitmun.common.domain.database.DatabaseConnection;

import java.sql.SQLException;

/**
 * Thrown to indicate that a {@link DatabaseConnection} has thrown a SQL exception.
 */
public class DatabaseSQLException extends RuntimeException {
  public DatabaseSQLException(SQLException exception) {
    super(exception);
  }
}
