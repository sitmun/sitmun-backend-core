package org.sitmun.administration.controller;

import java.util.Optional;
import org.sitmun.administration.service.access.ServiceAccessCheckExecutor;
import org.sitmun.administration.service.access.ServiceAccessObservation;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@BasePathAwareController
public class ServiceAccessCheckController {

  private final ServiceAccessCheckExecutor executor;

  public ServiceAccessCheckController(ServiceAccessCheckExecutor executor) {
    this.executor = executor;
  }

  @PostMapping("/services/{id}/access-check")
  @ResponseBody
  public ResponseEntity<ServiceAccessObservation> accessCheck(@PathVariable Integer id) {
    try {
      Optional<ServiceAccessObservation> observation = executor.check(id);
      return observation
          .map(ResponseEntity::ok)
          .orElseGet(() -> ResponseEntity.noContent().build());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
  }
}
