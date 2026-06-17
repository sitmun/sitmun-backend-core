package org.sitmun.administration.service.database;

import jakarta.validation.constraints.NotNull;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.administration.service.database.tester.DatabaseSQLException;
import org.sitmun.domain.database.DatabaseConnection;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DatabaseConnectionService {

  /** Test if the connection parameters is correct. */
  public List<Map<String, Object>> executeQuery(
      @NotNull DatabaseConnection connection, @NotNull String query) throws DatabaseSQLException {
    return executeQuery(connection, query, List.of());
  }

  /** Execute a query binding positional parameters when the SQL contains '?' placeholders. */
  public List<Map<String, Object>> executeQuery(
      @NotNull DatabaseConnection connection,
      @NotNull String query,
      @NotNull List<String> parameters)
      throws DatabaseSQLException {
    List<Map<String, Object>> result = new ArrayList<>();
    try (Connection con = openConnection(connection)) {
      if (parameters.isEmpty()) {
        executeStatement(con, query, result);
      } else {
        executePreparedStatement(con, query, parameters, result);
      }
    } catch (SQLException | ClassNotFoundException e) {
      log.error("Error getting connection: {}", e.getMessage(), e);
      if (e instanceof SQLException sqlException) {
        throw new DatabaseSQLException(sqlException);
      }
      throw new IllegalStateException(e);
    }
    return result;
  }

  private Connection openConnection(DatabaseConnection connection)
      throws SQLException, ClassNotFoundException {
    Class.forName(connection.getDriver());
    return DriverManager.getConnection(
        connection.getUrl(), connection.getUser(), connection.getPassword());
  }

  private void executeStatement(
      Connection connection, String query, List<Map<String, Object>> result)
      throws DatabaseSQLException {
    try (Statement stmt = connection.createStatement()) {
      retrieveResultSetMetadata(stmt, query, result);
    } catch (SQLException e) {
      log.error("Error in connection: {}", e.getMessage(), e);
      throw new DatabaseSQLException(e);
    }
  }

  private void executePreparedStatement(
      Connection connection,
      String query,
      List<String> parameters,
      List<Map<String, Object>> result)
      throws DatabaseSQLException {
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      for (int index = 0; index < parameters.size(); index++) {
        stmt.setString(index + 1, parameters.get(index));
      }
      retrieveResultSetMetadata(stmt, result);
    } catch (SQLException e) {
      log.error("Error in prepared statement: {}", e.getMessage(), e);
      throw new DatabaseSQLException(e);
    }
  }

  private void retrieveResultSetMetadata(
      Statement stmt, String query, List<Map<String, Object>> result) throws SQLException {
    try (ResultSet resultSet = stmt.executeQuery(query)) {
      appendRows(resultSet, result);
    }
  }

  private void retrieveResultSetMetadata(PreparedStatement stmt, List<Map<String, Object>> result)
      throws SQLException {
    try (ResultSet resultSet = stmt.executeQuery()) {
      appendRows(resultSet, result);
    }
  }

  private void appendRows(ResultSet resultSet, List<Map<String, Object>> result)
      throws SQLException {
    ResultSetMetaData metadata = resultSet.getMetaData();
    while (resultSet.next()) {
      Map<String, Object> row = new HashMap<>();
      for (int i = 1; i <= metadata.getColumnCount(); i++) {
        Object value = resultSet.getObject(i);
        row.put(metadata.getColumnLabel(i), value);
      }
      result.add(row);
    }
  }
}
