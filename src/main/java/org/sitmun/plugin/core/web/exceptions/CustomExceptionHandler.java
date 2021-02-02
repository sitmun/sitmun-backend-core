package org.sitmun.plugin.core.web.exceptions;

import org.sitmun.plugin.core.domain.DatabaseConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Controller advice.
 */
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Handles a {@link DatabaseConnection} driver missing exception.
   *
   * @param exception the exception.
   * @return a 500 error
   */
  @ExceptionHandler(DatabaseConnectionDriverNotFoundException.class)
  public ResponseEntity<ApiError> databaseConnectionDriverNotFound(DatabaseConnectionDriverNotFoundException exception) {
    ApiError error = new ApiError();
    error.setError("Driver not found");
    error.setMessage(exception.getCause().getLocalizedMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Handles a {@link DatabaseConnection} SQL exception.
   *
   * @param exception the exception.
   * @return a 500 error
   */
  @ExceptionHandler(DatabaseSQLException.class)
  public ResponseEntity<ApiError> databaseSQLException(DatabaseSQLException exception) {
    ApiError error = new ApiError();
    error.setError("SQL exception");
    error.setMessage(exception.getCause().getLocalizedMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
