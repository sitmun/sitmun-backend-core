package org.sitmun.infrastructure.web.controller;

/**
 * Classifies {@link org.springframework.dao.DataIntegrityViolationException} causes from SQL state
 * and constraint names.
 */
final class DataIntegrityConstraintDetector {

  private DataIntegrityConstraintDetector() {}

  /**
   * Detects foreign key constraint violations from SQL state or constraint name patterns.
   *
   * <p>PostgreSQL uses SQL state {@code 23503}. When the state is unavailable, falls back to
   * constraint names containing {@code _FK_}, {@code FK_}, or {@code FOREIGN}.
   */
  static boolean isForeignKeyViolation(String sqlState, String constraintName) {
    if (sqlState != null) {
      // 23505 / 23001 = unique violations — do not treat as FK even if the name contains FK_
      if ("23505".equals(sqlState) || "23001".equals(sqlState)) {
        return false;
      }
      if ("23503".equals(sqlState)) {
        return true;
      }
    }
    if (constraintName == null) {
      return false;
    }
    String upperConstraintName = constraintName.toUpperCase();
    return upperConstraintName.contains("_FK_")
        || upperConstraintName.contains("FK_")
        || upperConstraintName.contains("FOREIGN");
  }
}
