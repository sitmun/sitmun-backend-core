package org.sitmun.administration.service.database;

import lombok.extern.slf4j.Slf4j;

import org.sitmun.administration.service.database.tester.DatabaseSQLException;
import org.sitmun.domain.database.DatabaseConnection;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DatabaseConnectionService {

  /**
   * Test if the connection parameters is correct.
   */
  public List<Map<String, Object>> executeQuery(@NotNull DatabaseConnection connection, @NotNull String query) throws DatabaseSQLException {    
    List<Map<String, Object>> result = new ArrayList<>();
    Connection con = null;
    try {
      Class.forName(connection.getDriver());
      con = DriverManager.getConnection(connection.getUrl(), connection.getUser(), connection.getPassword());
      executeStatement(con, query, result);
    } catch (SQLException | ClassNotFoundException e) {
      log.error("Error getting connection: {}", e.getMessage(), e);
    }
    return result;
  }

  private void executeStatement(Connection connection, String query, List<Map<String, Object>> result) {
    try (Statement stmt = connection.createStatement()) {
      retrieveResultSetMetadata(stmt, query, result);
    } catch (SQLException e) {
      log.error("Error in connection: {}", e.getMessage(), e);
    }
  }

  private void retrieveResultSetMetadata(Statement stmt, String query, List<Map<String, Object>> result) {
    try (ResultSet resultSet = stmt.executeQuery(query)) {
      ResultSetMetaData metadata = resultSet.getMetaData();
      while (resultSet.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
          Object value = resultSet.getObject(i);
          row.put(metadata.getColumnLabel(i), value);
        }
        result.add(row);
      }
    } catch (SQLException e) {
      log.error("Error in statement: {}", e.getMessage(), e);
    }
  }
}
