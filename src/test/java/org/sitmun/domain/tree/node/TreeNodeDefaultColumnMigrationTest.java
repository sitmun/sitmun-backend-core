package org.sitmun.domain.tree.node;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@DisplayName("TreeNode TNO_DEFAULT column migration")
class TreeNodeDefaultColumnMigrationTest {

  private static final Path MIGRATION_CHANGELOG_ROOT =
      Path.of("../../../profiles/development/backend/liquibase").toAbsolutePath().normalize();

  @Test
  @DisplayName("adds TNO_DEFAULT column with NOT NULL DEFAULT FALSE; TNO_VISIBLE absent")
  void addsTnoDefaultColumn() throws Exception {
    String databaseUrl = newDatabaseUrl();
    try (Connection connection = openConnection(databaseUrl)) {
      createPre52Schema(connection);
      JdbcTemplate jdbc = jdbc(connection);
      assertThat(columnNames(jdbc)).doesNotContain("TNO_DEFAULT");
      assertThat(columnNames(jdbc)).doesNotContain("TNO_VISIBLE");
    }

    try (Connection connection = openConnection(databaseUrl)) {
      runMigration52(connection);
    }

    try (Connection connection = openConnection(databaseUrl)) {
      JdbcTemplate jdbc = jdbc(connection);
      assertThat(columnNames(jdbc)).contains("TNO_DEFAULT");
      assertThat(columnNames(jdbc)).doesNotContain("TNO_VISIBLE");
      assertThat(columnNames(jdbc)).doesNotContain("TNO_LOAD_BY_DEFAULT");
      assertThat(
              jdbc.queryForObject(
                  "SELECT TNO_DEFAULT FROM STM_TREE_NOD WHERE TNO_ID = 1", Boolean.class))
          .isFalse();
    }
  }

  @Test
  @DisplayName("is idempotent when TNO_DEFAULT already exists")
  void idempotentWhenColumnAlreadyExists() throws Exception {
    String databaseUrl = newDatabaseUrl();
    try (Connection connection = openConnection(databaseUrl)) {
      createPre52Schema(connection);
      runMigration52(connection);
    }

    try (Connection connection = openConnection(databaseUrl)) {
      runMigration52(connection);
    }

    try (Connection connection = openConnection(databaseUrl)) {
      assertThat(columnNames(jdbc(connection))).contains("TNO_DEFAULT");
    }
  }

  private static String newDatabaseUrl() {
    return "jdbc:h2:mem:migration52-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
  }

  private static Connection openConnection(String databaseUrl) throws SQLException {
    return DriverManager.getConnection(databaseUrl, "sa", "");
  }

  private static JdbcTemplate jdbc(Connection connection) {
    return new JdbcTemplate(new SingleConnectionDataSource(connection, true));
  }

  private static void createPre52Schema(Connection connection) {
    JdbcTemplate jdbc = jdbc(connection);
    jdbc.execute(
        """
        CREATE TABLE STM_TREE (
          TRE_ID INTEGER PRIMARY KEY,
          TRE_TYPE VARCHAR(50)
        )
        """);
    jdbc.execute(
        """
        CREATE TABLE STM_TREE_NOD (
          TNO_ID      INTEGER PRIMARY KEY,
          TNO_NAME    VARCHAR(80) NOT NULL,
          TNO_ACTIVE  BOOLEAN NOT NULL DEFAULT TRUE,
          TNO_RADIO   BOOLEAN,
          TNO_TREEID  INTEGER NOT NULL
        )
        """);
    jdbc.update("INSERT INTO STM_TREE (TRE_ID, TRE_TYPE) VALUES (1, 'cartography')");
    jdbc.update(
        "INSERT INTO STM_TREE_NOD (TNO_ID, TNO_NAME, TNO_ACTIVE, TNO_TREEID) VALUES (1, 'node', TRUE, 1)");
  }

  private static void runMigration52(Connection connection)
      throws LiquibaseException, SQLException, IOException {
    Database database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    try (Liquibase liquibase =
        new Liquibase(
            "changelog/52_add_tree_node_default.yaml",
            new DirectoryResourceAccessor(MIGRATION_CHANGELOG_ROOT),
            database)) {
      liquibase.update(new Contexts("dev"));
    }
  }

  private static Iterable<String> columnNames(JdbcTemplate jdbc) {
    return jdbc.query(
        """
        SELECT COLUMN_NAME
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME = 'STM_TREE_NOD'
        ORDER BY COLUMN_NAME
        """,
        (rs, rowNum) -> rs.getString("COLUMN_NAME"));
  }
}
