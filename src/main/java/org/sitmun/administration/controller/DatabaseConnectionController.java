package org.sitmun.administration.controller;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.sitmun.administration.service.database.tester.DatabaseConnectionDriverNotFoundException;
import org.sitmun.administration.service.database.tester.DatabaseConnectionTesterService;
import org.sitmun.administration.service.database.tester.DatabaseSQLException;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.database.DatabaseConnectionRepository;
import org.sitmun.infrastructure.web.dto.DomainExceptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@BasePathAwareController
public class DatabaseConnectionController {

  private final DatabaseConnectionRepository repository;

  private final DatabaseConnectionTesterService service;

  @Autowired
  public DatabaseConnectionController(
      DatabaseConnectionRepository repository, DatabaseConnectionTesterService service) {
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
  @ResponseBody
  public ResponseEntity<String> testConnection(@PathVariable("id") Integer id) {
    Optional<DatabaseConnection> connectionOp = repository.findById(id);
    if (connectionOp.isPresent()) {
      DatabaseConnection connection = connectionOp.get();
      service.testDriver(connection);
      service.testConnection(connection);
      return ResponseEntity.ok().build();
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Test if a potential {@link DatabaseConnection} is valid.
   *
   * @return 200 if valid
   */
  @PostMapping("/connections/test")
  @ResponseBody
  public ResponseEntity<String> testConnection(
      @NotNull @RequestBody DatabaseConnection connection) {
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
  public ResponseEntity<DomainExceptionResponse> databaseConnectionDriverNotFound(
      DatabaseConnectionDriverNotFoundException exception, @NonNull WebRequest request) {
    DomainExceptionResponse response =
        DomainExceptionResponse.builder()
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
  public ResponseEntity<DomainExceptionResponse> databaseSQLException(
      DatabaseSQLException exception, @NonNull WebRequest request) {
    DomainExceptionResponse response =
        DomainExceptionResponse.builder()
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message(exception.getLocalizedMessage())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(LocalDateTime.now(ZoneOffset.UTC))
            .path(((ServletWebRequest) request).getRequest().getRequestURI())
            .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
