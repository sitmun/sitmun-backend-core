package org.sitmun.feature.admin.database.tester;

import org.sitmun.common.domain.database.DatabaseConnection;
import org.sitmun.common.domain.database.DatabaseConnectionRepository;
import org.sitmun.common.web.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@BasePathAwareController
public class DatabaseConnectionController {

  private final DatabaseConnectionRepository repository;

  private final DatabaseConnectionTesterService service;

  @Autowired
  public DatabaseConnectionController(DatabaseConnectionRepository repository, DatabaseConnectionTesterService service) {
    this.repository = repository;
    this.service = service;
  }

  /**
   * Test if a {@link DatabaseConnection} is valid.
   *
   * @param id the identifier of the {@link DatabaseConnection}
   * @return 200 if valid
   */
  @GetMapping("/connections/{id}/test")
  public @ResponseBody
  ResponseEntity<String> testConnection(@PathVariable("id") Integer id) {
    Optional<DatabaseConnection> connectionOp = repository.findById(id);
    if (connectionOp.isPresent()) {
      DatabaseConnection connection = connectionOp.get();
      service.testDriver(connection);
      service.testConnection(connection);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }


  /**
   * Test if a potential {@link DatabaseConnection} is valid.
   *
   * @return 200 if valid
   */
  @PostMapping("/connections/test")
  public @ResponseBody
  ResponseEntity<String> testConnection(@NotNull @RequestBody DatabaseConnection connection) {
    service.testDriver(connection);
    service.testConnection(connection);
    return ResponseEntity.ok().build();
  }

  /**
   * Handles a {@link DatabaseConnection} driver missing exception.
   *
   * @param exception the exception.
   * @return a 500 error
   */
  @ExceptionHandler(DatabaseConnectionDriverNotFoundException.class)
  public ResponseEntity<ErrorResponse> databaseConnectionDriverNotFound(DatabaseConnectionDriverNotFoundException exception, @NonNull WebRequest request) {
    ErrorResponse response = ErrorResponse.builder()
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * Handles a {@link DatabaseConnection} SQL exception.
   *
   * @param exception the exception.
   * @return a 500 error
   */
  @ExceptionHandler(DatabaseSQLException.class)
  public ResponseEntity<ErrorResponse> databaseSQLException(DatabaseSQLException exception, @NonNull WebRequest request) {
    ErrorResponse response = ErrorResponse.builder()
      .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
      .message(exception.getLocalizedMessage())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .timestamp(LocalDateTime.now(ZoneOffset.UTC))
      .path(((ServletWebRequest) request).getRequest().getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
