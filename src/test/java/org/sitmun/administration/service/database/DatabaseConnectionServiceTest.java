package org.sitmun.administration.service.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.database.DatabaseConnection;

@DisplayName("Database Connection Service")
class DatabaseConnectionServiceTest {

  private final DatabaseConnectionService sut = new DatabaseConnectionService();

  @Test
  @DisplayName("executeQuery lowercases H2 column labels for template aliases")
  void executeQueryLowercasesH2ColumnLabels() throws Exception {
    String url = "jdbc:h2:mem:db-conn-label-case;DB_CLOSE_DELAY=-1";
    try (Connection connection = DriverManager.getConnection(url, "sa", "");
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE STM_LANGUAGE (LAN_ID INT, LAN_NAME VARCHAR(64))");
      statement.execute("INSERT INTO STM_LANGUAGE VALUES (1, 'Catalan')");
    }

    DatabaseConnection databaseConnection =
        DatabaseConnection.builder()
            .driver("org.h2.Driver")
            .url(url)
            .user("sa")
            .password("")
            .build();

    List<Map<String, Object>> rows =
        sut.executeQuery(
            databaseConnection,
            "SELECT LAN_ID AS lan_id, LAN_NAME AS lan_name FROM STM_LANGUAGE");

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0))
        .containsEntry("lan_id", 1)
        .containsEntry("lan_name", "Catalan")
        .doesNotContainKey("LAN_ID")
        .doesNotContainKey("LAN_NAME");
  }
}
