package org.sitmun.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;

@DisplayName("DataIntegrityConstraintDetector")
class DataIntegrityConstraintDetectorTest {

  @ParameterizedTest
  @CsvSource({
    "23503,STM_TAS_FK_SER,true",
    "23505,STM_TAS_FK_SER,false",
    ",STM_TAS_FK_SER,true",
    ",STM_APP_UK_NAME,false",
    ",STM_SOME_OTHER,false"
  })
  @DisplayName("isForeignKeyViolation classifies SQL state and constraint names")
  void isForeignKeyViolation(String sqlState, String constraintName, boolean expected) {
    assertThat(
            DataIntegrityConstraintDetector.isForeignKeyViolation(
                blankToNull(sqlState), constraintName))
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("isDuplicateKey returns false for PostgreSQL foreign key violations")
  void isDuplicateKeyReturnsFalseForForeignKeySqlState() throws Exception {
    assertThat(invokeIsDuplicateKey(dataIntegrityException("23503", "STM_TAS_FK_SER"))).isFalse();
  }

  @Test
  @DisplayName("isDuplicateKey returns true for PostgreSQL unique violations")
  void isDuplicateKeyReturnsTrueForUniqueSqlState() throws Exception {
    assertThat(invokeIsDuplicateKey(dataIntegrityException("23505", "STM_APP_UK_NAME"))).isTrue();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static DataIntegrityViolationException dataIntegrityException(
      String sqlState, String constraintName) {
    SQLException sqlException = new SQLException("violation", sqlState);
    var constraintViolation =
        new ConstraintViolationException("violation", sqlException, constraintName);
    return new DataIntegrityViolationException("violation", constraintViolation);
  }

  private static boolean invokeIsDuplicateKey(DataIntegrityViolationException exception)
      throws Exception {
    var method =
        DomainExceptionHandler.class.getDeclaredMethod(
            "isDuplicateKey", DataIntegrityViolationException.class);
    method.setAccessible(true);
    return (boolean) method.invoke(null, exception);
  }
}
