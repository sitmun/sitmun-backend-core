package org.sitmun.authorization.proxy.protocols.jdbc;

/**
 * Appends a row limit to proxy SQL. PostgreSQL, Oracle 12c and later, and H2 accept {@code OFFSET n
 * ROWS FETCH NEXT m ROWS ONLY}.
 */
public final class JdbcSqlDialect {

  private JdbcSqlDialect() {}

  /**
   * Appends pagination to {@code sql}. Null {@code limit} or {@code offset} omits that part. The
   * numbers are written as decimal digits.
   */
  public static String appendPagination(String sql, Integer limit, Integer offset) {
    if (limit == null && offset == null) {
      return sql;
    }
    StringBuilder builder = new StringBuilder(sql);
    if (offset != null) {
      builder.append(" OFFSET ").append(offset.intValue()).append(" ROWS");
    }
    if (limit != null) {
      builder.append(" FETCH NEXT ").append(limit.intValue()).append(" ROWS ONLY");
    }
    return builder.toString();
  }
}
