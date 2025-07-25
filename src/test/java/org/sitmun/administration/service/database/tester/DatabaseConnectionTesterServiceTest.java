package org.sitmun.administration.service.database.tester;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.database.DatabaseConnection;

import java.sql.SQLException;

@DisplayName("Database Connection Tester Service Test")
class DatabaseConnectionTesterServiceTest {

  private DatabaseConnectionTesterService sut;

  @BeforeEach
  void setup() {
    sut = new DatabaseConnectionTesterService();
  }

  @Test
  @DisplayName("Test driver with valid driver class")
  void testDriver() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.Driver").build();
    assertTrue(sut.testDriver(connection));
  }

  @Test
  @DisplayName("Test driver with invalid driver class should throw exception")
  void testDriverException() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.DriverX").build();
    DatabaseConnectionDriverNotFoundException exception =
        assertThrows(
            DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("org.h2.DriverX", exception.getCause().getLocalizedMessage());
  }

  @Test
  @DisplayName("Test driver with null driver should throw exception")
  void testNullDriverException() {
    DatabaseConnection connection = DatabaseConnection.builder().driver(null).build();
    DatabaseConnectionDriverNotFoundException exception =
        assertThrows(
            DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("null", exception.getCause().getLocalizedMessage());
  }

  @Test
  @DisplayName("Test connection with valid parameters")
  void testConnection() throws Exception {
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
  @DisplayName("Test connection with invalid URL should throw exception")
  void testConnectionException() {
    DatabaseConnection connection =
        DatabaseConnection.builder()
            .driver("org.h2.Driver")
            .url("jdb:h2:mem:testdb")
            .user("sa")
            .password(null)
            .build();
    sut.testDriver(connection);
    SQLException exception =
        assertThrows(SQLException.class, () -> sut.testConnection(connection));
    assertEquals(
        "No suitable driver found for jdb:h2:mem:testdb",
        exception.getLocalizedMessage());
  }
}
