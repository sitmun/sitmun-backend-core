package org.sitmun.authorization.proxy.protocols.jdbc;

import java.util.Locale;

/**
 * JDBC SQL dialect for proxy-side query shaping. Resolved from connection URL and/or driver class
 * name on {@link JdbcPayloadDto}.
 */
public enum JdbcSqlDialect {

  /**
   * PostgreSQL, H2, and other engines that accept trailing {@code LIMIT} / {@code OFFSET} clauses.
   */
  LIMIT_OFFSET,

  /**
   * Oracle 12c+ row limiting ({@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}, {@code FETCH NEXT …}).
   */
  ORACLE_FETCH;

  /**
   * Detects dialect from JDBC URL (preferred) and driver class name. Defaults to {@link
   * #LIMIT_OFFSET} when unknown.
   */
  public static JdbcSqlDialect fromJdbcMetadata(String driverClassName, String jdbcUrl) {
    String url = jdbcUrl != null ? jdbcUrl.toLowerCase(Locale.ROOT) : "";
    if (url.startsWith("jdbc:oracle:")) {
      return ORACLE_FETCH;
    }
    if (url.startsWith("jdbc:postgresql:") || url.startsWith("jdbc:h2:")) {
      return LIMIT_OFFSET;
    }
    String driver = driverClassName != null ? driverClassName.toLowerCase(Locale.ROOT) : "";
    if (driver.contains("oracle")) {
      return ORACLE_FETCH;
    }
    return LIMIT_OFFSET;
  }

  /**
   * Appends pagination clauses to {@code sql}. {@code limit} / {@code offset} are expected to be
   * numeric fragments (same contract as request parameters today).
   */
  public String appendPagination(String sql, String limit, String offset) {
    boolean hasLimit = hasText(limit);
    boolean hasOffset = hasText(offset);
    if (!hasLimit && !hasOffset) {
      return sql;
    }
    return switch (this) {
      case LIMIT_OFFSET -> appendLimitOffset(sql, limit, offset, hasLimit, hasOffset);
      case ORACLE_FETCH -> appendOracleFetch(sql, limit, offset, hasLimit, hasOffset);
    };
  }

  private static String appendLimitOffset(
      String sql, String limit, String offset, boolean hasLimit, boolean hasOffset) {
    StringBuilder b = new StringBuilder(sql);
    if (hasLimit) {
      b.append(" LIMIT ").append(limit);
    }
    if (hasOffset) {
      b.append(" OFFSET ").append(offset);
    }
    return b.toString();
  }

  private static String appendOracleFetch(
      String sql, String limit, String offset, boolean hasLimit, boolean hasOffset) {
    StringBuilder b = new StringBuilder(sql);
    if (hasOffset) {
      b.append(" OFFSET ").append(offset).append(" ROWS");
    }
    if (hasLimit) {
      b.append(" FETCH NEXT ").append(limit).append(" ROWS ONLY");
    }
    return b.toString();
  }

  private static boolean hasText(String s) {
    return s != null && !s.isBlank();
  }
}
