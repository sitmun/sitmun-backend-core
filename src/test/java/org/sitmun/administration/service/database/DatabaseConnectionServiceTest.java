package org.sitmun.administration.service.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.database.DatabaseConnection;

class DatabaseConnectionServiceTest {

  private final DatabaseConnectionService service = new DatabaseConnectionService();

  @Test
  void executeQueryBindsPreparedStatementParameters() {
    setupDatabase();

    DatabaseConnection connection =
        DatabaseConnection.builder()
            .driver("org.h2.Driver")
            .url("jdbc:h2:mem:template_test;DB_CLOSE_DELAY=-1")
            .user("sa")
            .password(null)
            .build();

    List<Map<String, Object>> rows =
        service.executeQuery(connection, "SELECT name FROM sample WHERE id = ?", List.of("1"));

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).containsEntry("NAME", "Parcela 23-A");
  }

  private void setupDatabase() {
    try {
      Class.forName("org.h2.Driver");
      try (Connection connection =
              DriverManager.getConnection(
                  "jdbc:h2:mem:template_test;DB_CLOSE_DELAY=-1", "sa", null);
          Statement statement = connection.createStatement()) {
        statement.execute("DROP TABLE IF EXISTS sample");
        statement.execute("CREATE TABLE sample(id INT PRIMARY KEY, name VARCHAR(255))");
        statement.execute("INSERT INTO sample(id, name) VALUES (1, 'Parcela 23-A')");
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }
}
