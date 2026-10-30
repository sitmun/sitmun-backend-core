package org.sitmun.infrastructure.persistence.dialect;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.CastType;

/**
 * Oracle {@code CURRENT_DATE} includes the time of day. JPA {@code current_date} is a calendar
 * date.
 *
 * <p>Without the {@link DialectResolutionInfo} constructor, Hibernate uses the no-argument one and
 * the dialect assumes the minimum Oracle version instead of the connected one.
 */
public class SitmunOracleDialect extends OracleDialect {

  public SitmunOracleDialect() {
    super();
  }

  public SitmunOracleDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  public String currentDate() {
    return "trunc(current_date)";
  }

  /** An Oracle {@code DATE} keeps the time of day, so a cast to date truncates it. */
  @Override
  public String castPattern(CastType from, CastType to) {
    String pattern = super.castPattern(from, to);
    return to == CastType.DATE ? "trunc(" + pattern + ")" : pattern;
  }
}
