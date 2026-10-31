package org.sitmun.infrastructure.persistence.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.CastType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SITMUN Oracle dialect")
class SitmunOracleDialectTest {

  @Test
  @DisplayName("The dialect reports the connected Oracle version")
  void dialectUsesConnectedVersion() throws Exception {
    Dialect dialect = build(21, 0);

    assertThat(dialect.getVersion().getMajor()).isEqualTo(21);
    assertThat(dialect.currentDate()).isEqualTo("trunc(current_date)");
  }

  @Test
  @DisplayName("A cast to date drops the time of day, as current_date does")
  void castToDateIsCalendarDate() throws Exception {
    Dialect dialect = build(21, 0);

    assertThat(dialect.castPattern(CastType.TIMESTAMP, CastType.DATE)).startsWith("trunc(");
  }

  /** Hibernate builds a configured dialect through this constructor when the class declares it. */
  private static Dialect build(int major, int minor) throws Exception {
    DialectResolutionInfo info = mock(DialectResolutionInfo.class, CALLS_REAL_METHODS);
    doReturn(major).when(info).getDatabaseMajorVersion();
    doReturn(minor).when(info).getDatabaseMinorVersion();
    doReturn(major + "." + minor).when(info).getDatabaseVersion();
    return SitmunOracleDialect.class.getConstructor(DialectResolutionInfo.class).newInstance(info);
  }
}
