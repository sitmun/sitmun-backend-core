package org.sitmun.domain.service.check;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A populated plain {@code TIMESTAMP(6)} must become {@code TIMESTAMP(6) WITH TIME ZONE} without
 * moving the instant. The stored clock time is UTC. The upgrade session is not.
 *
 * <p>Run: {@code ./gradlew testOracle --tests
 * org.sitmun.domain.service.check.ServiceCheckOracleObservedRebuildIT}
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Import(I18nTestConfiguration.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*oracle.*")
@DisplayName("Oracle service-check observed rebuild")
class ServiceCheckOracleObservedRebuildIT {

  private static final Instant CHECK_OBSERVED = Instant.parse("2026-10-05T12:34:56.123456Z");
  private static final Instant SAMPLE_OBSERVED = Instant.parse("2026-10-05T12:34:56.654321Z");
  private static final String CHANGELOG = "db/changelog/db.changelog-oracle.yaml";

  @Autowired private EntityManager entityManager;
  @Autowired private DataSource dataSource;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckRepository checkRepository;
  @Autowired private ServiceCheckSampleRepository sampleRepository;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("A Madrid session keeps the stored UTC instants")
  void madridSessionKeepsStoredUtcInstants() throws Exception {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    Saved saved =
        tx.execute(
            status -> {
              Service service =
                  serviceRepository.save(
                      Service.builder()
                          .name("Observed rebuild WMS")
                          .type("WMS")
                          .serviceURL("http://example.com/wms")
                          .blocked(false)
                          .isProxied(false)
                          .build());
              ServiceCheck check =
                  checkRepository.save(
                      ServiceCheck.builder()
                          .service(service)
                          .observer(ServiceCheckStore.BACKEND)
                          .status("up")
                          .statusRank(0)
                          .elapsedMs(12L)
                          .detail("ok")
                          .observedAt(CHECK_OBSERVED)
                          .build());
              ServiceCheckSample sample =
                  sampleRepository.save(
                      ServiceCheckSample.builder()
                          .service(service)
                          .observer(ServiceCheckStore.BACKEND)
                          .status("up")
                          .statusRank(0)
                          .elapsedMs(12L)
                          .observedAt(SAMPLE_OBSERVED)
                          .build());
              entityManager.flush();
              return new Saved(check.getId(), sample.getId());
            });

    rewindToPlainTimestampAndRerunLiquibase();

    tx.executeWithoutResult(
        status -> {
          entityManager.clear();
          assertThat(checkRepository.findById(saved.checkId()).orElseThrow().getObservedAt())
              .isEqualTo(CHECK_OBSERVED);
          assertThat(sampleRepository.findById(saved.sampleId()).orElseThrow().getObservedAt())
              .isEqualTo(SAMPLE_OBSERVED);
        });
  }

  private void rewindToPlainTimestampAndRerunLiquibase() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        statement.execute(rewindColumn("STM_SERVICE_CHECK", "SEC_OBSERVED", "SEC_OBSERVED_UTC"));
        statement.execute(
            """
            DECLARE
              l_idx NUMBER;
            BEGIN
              SELECT COUNT(*) INTO l_idx FROM user_indexes WHERE index_name = 'STM_SCS_IX_SER_OBS';
              IF l_idx = 1 THEN
                EXECUTE IMMEDIATE 'DROP INDEX STM_SCS_IX_SER_OBS';
              END IF;
            END;
            """);
        statement.execute(
            rewindColumn("STM_SERVICE_CHECK_SAMPLE", "SCS_OBSERVED", "SCS_OBSERVED_UTC"));
        int deleted =
            statement.executeUpdate(
                """
                DELETE FROM DATABASECHANGELOG
                WHERE ID LIKE '34-service-check-%rebuild-oracle'
                   OR ID LIKE '32-service-check-%observed%'
                """);
        assertThat(deleted).isEqualTo(6);
      }
      connection.commit();

      try (Statement statement = connection.createStatement()) {
        statement.execute("ALTER SESSION SET TIME_ZONE = 'Europe/Madrid'");
      }
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase =
          new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
        liquibase.update(new Contexts());
      }
    }
    try (Connection connection = dataSource.getConnection()) {
      assertColumn(connection, "STM_SERVICE_CHECK", "SEC_OBSERVED");
      assertColumn(connection, "STM_SERVICE_CHECK_SAMPLE", "SCS_OBSERVED");
      try (Statement statement = connection.createStatement();
          ResultSet indexes =
              statement.executeQuery(
                  """
                  SELECT COUNT(*) FROM user_indexes WHERE index_name = 'STM_SCS_IX_SER_OBS'
                  """)) {
        assertThat(indexes.next()).isTrue();
        assertThat(indexes.getInt(1)).isEqualTo(1);
      }
    }
  }

  private static String rewindColumn(String table, String column, String temporary) {
    return """
        DECLARE
          l_type VARCHAR2(128);
        BEGIN
          SELECT data_type INTO l_type
          FROM user_tab_columns
          WHERE table_name = '%s' AND column_name = '%s';
          IF l_type NOT LIKE '%%WITH TIME ZONE' THEN
            RETURN;
          END IF;
          EXECUTE IMMEDIATE 'ALTER TABLE %s ADD (%s TIMESTAMP(6))';
          EXECUTE IMMEDIATE 'UPDATE %s SET %s = CAST(SYS_EXTRACT_UTC(%s) AS TIMESTAMP(6))';
          EXECUTE IMMEDIATE 'ALTER TABLE %s DROP COLUMN %s';
          EXECUTE IMMEDIATE 'ALTER TABLE %s RENAME COLUMN %s TO %s';
          EXECUTE IMMEDIATE 'ALTER TABLE %s MODIFY (%s NOT NULL)';
        END;
        """
        .formatted(
            table, column, table, temporary, table, temporary, column, table, column, table,
            temporary, column, table, column);
  }

  private static void assertColumn(Connection connection, String table, String column)
      throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement(
            """
            SELECT data_type, nullable FROM user_tab_columns
            WHERE table_name = ? AND column_name = ?
            """)) {
      statement.setString(1, table);
      statement.setString(2, column);
      try (ResultSet rows = statement.executeQuery()) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getString(1)).isEqualTo("TIMESTAMP(6) WITH TIME ZONE");
        assertThat(rows.getString(2)).isEqualTo("N");
      }
    }
  }

  private record Saved(Integer checkId, Integer sampleId) {}
}
