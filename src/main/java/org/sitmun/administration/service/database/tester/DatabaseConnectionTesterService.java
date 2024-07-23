package org.sitmun.administration.service.database.tester;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.database.DatabaseConnection;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
@Slf4j
public class DatabaseConnectionTesterService {

  /**
   * Test if the driver is available.
   */
  public Boolean testDriver(@NotNull DatabaseConnection connection) throws DatabaseConnectionDriverNotFoundException {
    Integer id = connection.getId();
    String driver = connection.getDriver();
    try {
      Class.forName(driver).getDeclaredConstructor().newInstance();
    } catch (NullPointerException exception) {
      log.error("Driver not found for DatabaseConnection({}) with driver {}", id, driver, exception);
      throw new DatabaseConnectionDriverNotFoundException(new NullPointerException("null"));
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
             ClassNotFoundException exception) {
      log.error("Driver not found for DatabaseConnection({}) with driver \"{}\"", id, driver, exception);
      throw new DatabaseConnectionDriverNotFoundException(exception);
    }
    return true;
  }

  /**
   * Test if the connection parameters is correct.
   */
  public Boolean testConnection(@NotNull DatabaseConnection connection) throws DatabaseSQLException {
    Integer id = connection.getId();
    String url = connection.getUrl();
    String user = connection.getUser();
    String password = connection.getPassword();
    Connection con = null;
    DatabaseSQLException error = null;
    try {
      con = DriverManager.getConnection(url, user, password);
    } catch (SQLException exception) {
      log.error("GetConnection throws exception for DatabaseConnection({}) with url \"{}\"", id, url, exception);
      throw new DatabaseSQLException(exception);
    } finally {
      if (con != null) {
        try {
          con.close();
        } catch (SQLException exception) {
          log.error("Close throws exception for DatabaseConnection({}) with url \"{}\"", id, url, exception);
          error = new DatabaseSQLException(exception);
        }
      }
    }
    if (error != null) {
      throw error;
    }
    return true;
  }
}
