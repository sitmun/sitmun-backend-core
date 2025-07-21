package org.sitmun.administration.service.database.tester;

import java.sql.SQLException;
import org.sitmun.domain.database.DatabaseConnection;

/** Thrown to indicate that a {@link DatabaseConnection} has thrown a SQL exception. */
public class DatabaseSQLException extends RuntimeException {
  public DatabaseSQLException(SQLException exception) {
    super(exception);
  }
}
