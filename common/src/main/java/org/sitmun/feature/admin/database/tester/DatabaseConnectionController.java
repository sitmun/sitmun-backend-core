package org.sitmun.feature.admin.database.tester;

import org.sitmun.common.domain.database.DatabaseConnection;
import org.sitmun.common.domain.database.DatabaseConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
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
  @RequestMapping(method = RequestMethod.GET, path = "/connections/{id}/test")
  public @ResponseBody
  ResponseEntity<?> testConnection(@PathVariable("id") Integer id) {
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
  @RequestMapping(method = RequestMethod.POST, path = "/connections/test")
  public @ResponseBody
  ResponseEntity<?> testConnection(@NotNull @RequestBody DatabaseConnection connection) {
    service.testDriver(connection);
    service.testConnection(connection);
    return ResponseEntity.ok().build();
  }
}
