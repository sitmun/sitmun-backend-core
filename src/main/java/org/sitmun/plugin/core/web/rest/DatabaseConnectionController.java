package org.sitmun.plugin.core.web.rest;

import org.sitmun.plugin.core.domain.DatabaseConnection;
import org.sitmun.plugin.core.repository.DatabaseConnectionRepository;
import org.sitmun.plugin.core.service.DatabaseConnectionTesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@RepositoryRestController
@RequestMapping("/api")
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
}
