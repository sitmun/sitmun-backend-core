package org.sitmun.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApplicationContactMigrationTest")
class ApplicationContactMigrationTest {

  private static SpringLiquibase buildLiquibase(DataSource ds) throws Exception {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(ds);
    liquibase.setChangeLog("file:./config/db/changelog/db.changelog-master.yaml");
    return liquibase;
  }

  private static DataSource createFreshDataSource() {
    var ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    return ds;
  }

  @Test
  @DisplayName("column APP_RESPONSIBLE_INSTITUTION is added idempotently; APP_CREATORID unchanged")
  void migrationIsIdempotentAndColumnExists() throws Exception {
    var ds = createFreshDataSource();

    var lb = buildLiquibase(ds);
    lb.setDropFirst(true);
    lb.afterPropertiesSet();

    Integer normalUserId;
    Integer appId;
    try (var conn = ds.getConnection()) {
      insertTestData(conn);
      normalUserId = selectNormalUserId(conn);
      assertThat(normalUserId).isNotNull();

      appId =
          (Integer)
              querySingle(
                  conn, "SELECT APP_ID FROM STM_APP ORDER BY APP_ID LIMIT 1", rs -> rs.getInt(1));
      if (appId != null) {
        conn.createStatement()
            .execute(
                "UPDATE STM_APP SET APP_CREATORID = " + normalUserId + " WHERE APP_ID = " + appId);
      }
      assertThat(columnExists(conn, "STM_APP", "APP_RESPONSIBLE_INSTITUTION")).isTrue();
    }

    var lb2 = buildLiquibase(ds);
    lb2.afterPropertiesSet();

    try (var conn = ds.getConnection()) {
      if (appId != null) {
        Integer creatorId =
            (Integer)
                querySingle(
                    conn,
                    "SELECT APP_CREATORID FROM STM_APP WHERE APP_ID = " + appId,
                    rs -> rs.getObject(1, Integer.class));
        assertThat(creatorId).isEqualTo(normalUserId);
      }
      assertThat(columnExists(conn, "STM_APP", "APP_RESPONSIBLE_INSTITUTION")).isTrue();
    }

    var lb3 = buildLiquibase(ds);
    lb3.afterPropertiesSet();

    try (var conn = ds.getConnection()) {
      assertThat(columnExists(conn, "STM_APP", "APP_RESPONSIBLE_INSTITUTION")).isTrue();
    }
  }

  private void insertTestData(Connection conn) throws SQLException {
    conn.createStatement()
        .execute(
            "INSERT INTO STM_USER (USE_ID, USE_USER, USE_ADM, USE_BLOCKED) "
                + "VALUES (77777, 'migration-test-user', TRUE, FALSE)");
  }

  private Integer selectNormalUserId(Connection conn) throws SQLException {
    return (Integer)
        querySingle(
            conn,
            "SELECT USE_ID FROM STM_USER WHERE USE_USER = 'migration-test-user'",
            rs -> rs.getInt(1));
  }

  private boolean columnExists(Connection conn, String table, String column) throws SQLException {
    try (var rs =
        conn.getMetaData().getColumns(null, null, table.toUpperCase(), column.toUpperCase())) {
      return rs.next();
    }
  }

  @FunctionalInterface
  interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
  }

  private <T> T querySingle(Connection conn, String sql, RowMapper<T> mapper) throws SQLException {
    try (var stmt = conn.createStatement();
        var rs = stmt.executeQuery(sql)) {
      return rs.next() ? mapper.map(rs) : null;
    }
  }
}
