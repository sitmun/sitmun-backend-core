package org.sitmun.administration.service.database.tester;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.database.DatabaseConnection;

@DisplayName("Database Connection Tester Service Test")
class DatabaseConnectionTesterServiceTest {

  private DatabaseConnectionTesterService sut;

  @BeforeEach
  void setup() {
    sut = new DatabaseConnectionTesterService();
  }

  @Test
  @DisplayName("org.h2.Driver is accepted")
  void acceptsH2Driver() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.Driver").build();
    assertTrue(sut.testDriver(connection));
  }

  @Test
  @DisplayName("SQLServerDriver is accepted")
  void acceptsSqlServerDriver() {
    DatabaseConnection connection =
        DatabaseConnection.builder().driver("com.microsoft.sqlserver.jdbc.SQLServerDriver").build();
    assertTrue(sut.testDriver(connection));
  }

  @Test
  @DisplayName("Unknown driver class throws DatabaseConnectionDriverNotFoundException")
  void unknownDriverClassThrowsNotFound() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.DriverX").build();
    DatabaseConnectionDriverNotFoundException exception =
        assertThrows(
            DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("org.h2.DriverX", exception.getCause().getLocalizedMessage());
  }

  @Test
  @DisplayName("Null driver throws DatabaseConnectionDriverNotFoundException")
  void nullDriverThrowsNotFound() {
    DatabaseConnection connection = DatabaseConnection.builder().driver(null).build();
    DatabaseConnectionDriverNotFoundException exception =
        assertThrows(
            DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("null", exception.getCause().getLocalizedMessage());
  }

  @Test
  @DisplayName("H2 memory URL connects")
  void h2MemoryUrlConnects() throws Exception {
    DatabaseConnection connection =
        DatabaseConnection.builder()
            .driver("org.h2.Driver")
            .url("jdbc:h2:mem:testdb")
            .user("sa")
            .password(null)
            .build();
    sut.testDriver(connection);
    assertTrue(sut.testConnection(connection));
  }

  @Test
  @DisplayName("Malformed JDBC URL throws SQLException")
  void malformedJdbcUrlThrowsSqlException() {
    DatabaseConnection connection =
        DatabaseConnection.builder()
            .driver("org.h2.Driver")
            .url("jdb:h2:mem:testdb")
            .user("sa")
            .password(null)
            .build();
    sut.testDriver(connection);
    assertThrows(SQLException.class, () -> sut.testConnection(connection));
  }
}
