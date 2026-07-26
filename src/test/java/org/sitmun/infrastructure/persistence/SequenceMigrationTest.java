package org.sitmun.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SequenceMigrationTest {

  private static final List<Generator> GENERATORS =
      List.of(
          new Generator("ABC_ID", "STM_APP_BCKG", "ABC_ID"),
          new Generator("AGI_ID", "STM_AVAIL_GI", "AGI_ID"),
          new Generator("APP_ID", "STM_APP", "APP_ID"),
          new Generator("ATE_ID", "STM_APP_TER", "ATE_ID"),
          new Generator("ATS_ID", "STM_AVAIL_TSK", "ATS_ID"),
          new Generator("BAC_ID", "STM_BACKGRD", "BAC_ID"),
          new Generator("CNF_ID", "STM_CONF", "CNF_ID"),
          new Generator("COD_ID", "STM_CODELIST", "COD_ID"),
          new Generator("COM_ID", "STM_COMMENT", "COM_ID"),
          new Generator("CON_ID", "STM_CONNECT", "CON_ID"),
          new Generator("FGI_ID", "STM_FIL_GI", "FGI_ID"),
          new Generator("GEO_ID", "STM_GEOINFO", "GEO_ID"),
          new Generator("GGI_ID", "STM_GRP_GI", "GGI_ID"),
          new Generator("GTS_ID", "STM_GRP_TSK", "GTS_ID"),
          new Generator("GTT_ID", "STM_GTER_TYP", "GTT_ID"),
          new Generator("LAN_ID", "STM_LANGUAGE", "LAN_ID"),
          new Generator("LOG_ID", "STM_LOG", "LOG_ID"),
          new Generator("PAP_ID", "STM_PAR_APP", "PAP_ID"),
          new Generator("PGI_ID", "STM_PAR_GI", "PGI_ID"),
          new Generator("POS_ID", "STM_POST", "POS_ID"),
          new Generator("PSE_ID", "STM_PAR_SER", "PSE_ID"),
          new Generator("PSG_ID", "STM_PAR_SGI", "PSG_ID"),
          new Generator("ROL_ID", "STM_ROLE", "ROL_ID"),
          new Generator("SER_ID", "STM_SERVICE", "SER_ID"),
          new Generator("SGI_ID", "STM_STY_GI", "SGI_ID"),
          new Generator("TAR_ID", "STM_TASKREL", "TAR_ID"),
          new Generator("TAS_ID", "STM_TASK", "TAS_ID"),
          new Generator("TER_ID", "STM_TERRITORY", "TER_ID"),
          new Generator("TET_ID", "STM_TER_TYP", "TET_ID"),
          new Generator("TNO_ID", "STM_TREE_NOD", "TNO_ID"),
          new Generator("TRA_ID", "STM_TRANSLATION", "TRA_ID"),
          new Generator("TRE_ID", "STM_TREE", "TRE_ID"),
          new Generator("TUI_ID", "STM_TSK_UI", "TUI_ID"),
          new Generator("TTY_ID", "STM_TSK_TYP", "TTY_ID"),
          new Generator("UCO_ID", "STM_USR_CONF", "UCO_ID"),
          new Generator("USE_ID", "STM_USER", "USE_ID"),
          new Generator("USER_TOKEN_ID", "STM_TOKEN_USER", "USER_TOKEN_ID"));

  @Test
  void productionChangelogInitializesEveryTableGeneratorAboveSeededIds() throws Exception {
    var dataSource = createDataSource();

    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("file:./config/db/changelog/db.changelog-master.yaml");
    liquibase.setDropFirst(true);
    liquibase.afterPropertiesSet();

    try (var connection = dataSource.getConnection()) {
      assertSoftly(
          softly ->
              GENERATORS.forEach(
                  generator -> {
                    try {
                      var expected = selectNextId(connection, generator);
                      var actual = selectSequenceCount(connection, generator.sequenceName());
                      softly
                          .assertThat(actual)
                          .as("%s generator", generator.sequenceName())
                          .isEqualTo(expected);
                    } catch (SQLException exception) {
                      throw new AssertionError(generator.sequenceName(), exception);
                    }
                  }));
    }
  }

  private static DataSource createDataSource() {
    var profiles = System.getProperty("spring.profiles.active", "test,h2");
    if (profiles.contains("postgres")) {
      return new DriverManagerDataSource(
          "jdbc:postgresql://localhost:5432/sitmun3", "sitmun3", "sitmun3");
    }
    if (profiles.contains("oracle")) {
      return new DriverManagerDataSource(
          "jdbc:oracle:thin:@//localhost:1521/sitmun3", "sitmun3", "sitmun3");
    }

    var dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:sequence-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    return dataSource;
  }

  private static long selectNextId(Connection connection, Generator generator) throws SQLException {
    try (var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT COALESCE(MAX("
                    + generator.idColumn()
                    + "), 0) + 1 FROM "
                    + generator.tableName())) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }

  private static Long selectSequenceCount(Connection connection, String sequenceName)
      throws SQLException {
    try (var statement =
        connection.prepareStatement("SELECT SEQ_COUNT FROM STM_SEQUENCE WHERE SEQ_NAME = ?")) {
      statement.setString(1, sequenceName);
      try (var result = statement.executeQuery()) {
        return result.next() ? result.getLong(1) : null;
      }
    }
  }

  private record Generator(String sequenceName, String tableName, String idColumn) {}
}
