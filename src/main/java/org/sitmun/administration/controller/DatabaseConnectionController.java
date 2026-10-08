package org.sitmun.administration.controller;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.sitmun.administration.controller.dto.DatabaseConnectionDto;
import org.sitmun.administration.service.database.tester.DatabaseConnectionTesterService;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.database.DatabaseConnectionRepository;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@BasePathAwareController
public class DatabaseConnectionController {

  private final DatabaseConnectionRepository repository;

  private final DatabaseConnectionTesterService service;

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
  public ResponseEntity<DatabaseConnectionDto> testConnection(@PathVariable Integer id) {
    Optional<DatabaseConnection> connectionOp = repository.findById(id);
    if (connectionOp.isPresent()) {
      DatabaseConnection connection = connectionOp.get();
      return testConnection(connection);
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
  public ResponseEntity<DatabaseConnectionDto> testConnection(
      @NotNull @RequestBody DatabaseConnection connection) {
    try {
      service.testDriver(connection);
      service.testConnection(connection);
    } catch (Exception e) {
      return ResponseEntity.badRequest()
          .body(
              DatabaseConnectionDto.builder()
                  .isValid(false)
                  .message(e.getLocalizedMessage())
                  .build());
    }
    return ResponseEntity.ok(DatabaseConnectionDto.builder().isValid(true).build());
  }
}
