package org.sitmun.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.DatabaseConnection;
import org.sitmun.web.exceptions.DatabaseConnectionDriverNotFoundException;
import org.sitmun.web.exceptions.DatabaseSQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTesterServiceTest {

  private DatabaseConnectionTesterService sut;

  @BeforeEach
  public void setup() {
    sut = new DatabaseConnectionTesterService();
  }

  @Test
  public void testDriver() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.Driver").build();
    assertTrue(sut.testDriver(connection));
  }

  @Test
  public void testDriverException() {
    DatabaseConnection connection = DatabaseConnection.builder().driver("org.h2.DriverX").build();
    DatabaseConnectionDriverNotFoundException exception = assertThrows(DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("org.h2.DriverX", exception.getCause().getLocalizedMessage());
  }

  @Test
  public void testNullDriverException() {
    DatabaseConnection connection = DatabaseConnection.builder().driver(null).build();
    DatabaseConnectionDriverNotFoundException exception = assertThrows(DatabaseConnectionDriverNotFoundException.class, () -> sut.testDriver(connection));
    assertEquals("null", exception.getCause().getLocalizedMessage());
  }

  @Test
  public void testConnection() {
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
  public void testConnectionException() {
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