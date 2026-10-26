package org.sitmun.domain.service.check;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.time.Instant;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Oracle service-check observed columns")
class ServiceCheckOracleObservedColumnTest {

  private static final Path MODULE = Path.of("").toAbsolutePath().normalize();
  private static final Path STACK = MODULE.getParent().getParent().getParent();
  private static final String OBSERVED_TYPE = "TIMESTAMP(6) WITH TIME ZONE";
  private static final String SEC_MODIFY =
      "ALTER TABLE STM_SERVICE_CHECK MODIFY (SEC_OBSERVED " + OBSERVED_TYPE + ")";
  private static final String SCS_MODIFY =
      "ALTER TABLE STM_SERVICE_CHECK_SAMPLE MODIFY (SCS_OBSERVED " + OBSERVED_TYPE + ")";

  @Test
  @DisplayName("OracleDialect maps Instant to TIMESTAMP WITH TIME ZONE by default")
  void oracleDialectMapsInstantToTimestampWithTimeZone() {
    StandardServiceRegistry registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", OracleDialect.class.getName())
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
            .build();
    try {
      Metadata metadata =
          new MetadataSources(registry)
              .addAnnotatedClass(InstantJdbcProbe.class)
              .getMetadataBuilder()
              .build();
      PersistentClass binding =
          metadata.getEntityBindings().stream()
              .filter(entity -> InstantJdbcProbe.class.getName().equals(entity.getClassName()))
              .findFirst()
              .orElseThrow();
      BasicValue observed = (BasicValue) binding.getProperty("observed").getValue();
      int jdbcTypeCode = observed.resolve().getJdbcType().getJdbcTypeCode();

      assertThat(jdbcTypeCode).isEqualTo(Types.TIMESTAMP_WITH_TIMEZONE);
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  @Test
  @DisplayName("Oracle changelog widens observed columns and H2/PostgreSQL creates stay TIMESTAMP")
  void oracleChangelogWidensObservedColumns() throws Exception {
    String master =
        Files.readString(MODULE.resolve("config/db/changelog/db.changelog-master.yaml"));
    String oracleTests =
        Files.readString(
            MODULE.resolve("src/test/resources/db/changelog/db.changelog-oracle.yaml"));
    String creates = Files.readString(MODULE.resolve("config/db/changelog/25_service_check.yaml"));
    String incremental =
        Files.readString(
            MODULE.resolve("config/db/changelog/32_service_check_observed_timestamptz.yaml"));

    assertThat(master).contains("32_service_check_observed_timestamptz.yaml");
    assertThat(oracleTests).contains("32_service_check_observed_timestamptz.yaml");
    assertObservedAlter(incremental);
    assertPlainTimestampCreates(creates);

    assertObservedAlter(
        readProfile(
            "profiles/oracle/liquibase/changelog/33_service_check_observed_timestamptz.yaml"));
    assertThat(Files.readString(STACK.resolve("profiles/oracle/liquibase/master.xml")))
        .contains("changelog/33_service_check_observed_timestamptz.yaml");
    assertPlainTimestampCreates(
        readProfile("profiles/oracle/liquibase/changelog/28_service_check.yaml"));

    assertObservedAlter(
        readProfile(
            "profiles/postgres/liquibase/changelog/33_service_check_observed_timestamptz.yaml"));
    assertThat(Files.readString(STACK.resolve("profiles/postgres/liquibase/master.xml")))
        .contains("changelog/33_service_check_observed_timestamptz.yaml");
    assertPlainTimestampCreates(
        readProfile("profiles/postgres/liquibase/changelog/28_service_check.yaml"));

    assertObservedAlter(
        readProfile(
            "profiles/development/backend/liquibase/changelog/89_service_check_observed_timestamptz.yaml"));
    assertThat(Files.readString(STACK.resolve("profiles/development/backend/liquibase/master.xml")))
        .contains("changelog/89_service_check_observed_timestamptz.yaml");
    assertPlainTimestampCreates(
        readProfile("profiles/development/backend/liquibase/changelog/80_service_check.yaml"));
  }

  private static void assertObservedAlter(String changelog) {
    assertThat(changelog).contains("dbms: oracle");
    assertThat(changelog).contains("onFail: MARK_RAN");
    assertThat(changelog).contains("USER_TAB_COLUMNS");
    assertThat(changelog).contains("DATA_TYPE = '" + OBSERVED_TYPE + "'");
    assertThat(changelog).contains(SEC_MODIFY);
    assertThat(changelog).contains(SCS_MODIFY);
    assertThat(changelog).contains("DROP INDEX STM_SCS_IX_SER_OBS");
    assertThat(changelog)
        .contains(
            "CREATE INDEX STM_SCS_IX_SER_OBS ON STM_SERVICE_CHECK_SAMPLE (SCS_SERID, SCS_OBSERVED)");
  }

  private static void assertPlainTimestampCreates(String changelog) {
    assertThat(changelog).contains("SEC_OBSERVED TIMESTAMP NOT NULL");
    assertThat(changelog).contains("SCS_OBSERVED TIMESTAMP NOT NULL");
    assertThat(changelog).contains("SEC_OBSERVED TIMESTAMP(6) NOT NULL");
    assertThat(changelog).contains("SCS_OBSERVED TIMESTAMP(6) NOT NULL");
    assertThat(changelog).doesNotContain("WITH TIME ZONE");
  }

  private static String readProfile(String relativePath) throws Exception {
    return Files.readString(STACK.resolve(relativePath));
  }
}

@Entity(name = "InstantJdbcProbe")
class InstantJdbcProbe {
  @Id private Long id;

  private Instant observed;
}
