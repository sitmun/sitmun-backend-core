package org.sitmun.feature.database.tester;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.common.domain.database.DatabaseConnection;
import org.sitmun.feature.admin.database.tester.DatabaseConnectionDriverNotFoundException;
import org.sitmun.feature.admin.database.tester.DatabaseConnectionTesterService;
import org.sitmun.feature.admin.database.tester.DatabaseSQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTesterServiceTest {

  private DatabaseConnectionTesterService sut;

  @BeforeEach
  void setup() {
    sut = new DatabaseConnectionTesterService();
  }

  @Test
  void testDriver() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.Driver").build();
    assertTrue(sut.testDriver(connection));
  }

  @Test
  void testDriverException() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.DriverX").build();
    DatabaseConnectionDriverNotFoundException exception = assertThrows(DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("org.h2.DriverX", exception.getCause().getLocalizedMessage());
  }

  @Test
  void testNullDriverException() {
    DatabaseConnection connection = DatabaseConnection.builder().driver(null).build();
    DatabaseConnectionDriverNotFoundException exception = assertThrows(DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("null", exception.getCause().getLocalizedMessage());
  }

  @Test
  void testConnection() {
    DatabaseConnection connection = DatabaseConnection.builder()
      .driver("org.h2.Driver")
      .url("jdbc:h2:mem:testdb")
      .name("sa")
      .password("password")
      .build();
    sut.testDriver(connection);
    assertTrue(sut.testConnection(connection));
  }

  @Test
  void testConnectionException() {
    DatabaseConnection connection = DatabaseConnection.builder()
      .driver("org.h2.Driver")
      .url("jdb:h2:mem:testdb")
      .name("sa")
      .password("password")
      .build();
    sut.testDriver(connection);
    DatabaseSQLException exception = assertThrows(DatabaseSQLException.class, () -> sut.testConnection(connection));
    assertEquals("No suitable driver found for jdb:h2:mem:testdb", exception.getCause().getLocalizedMessage());
  }
}