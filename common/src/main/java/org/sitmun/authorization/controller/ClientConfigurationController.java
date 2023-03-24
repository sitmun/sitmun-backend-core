package org.sitmun.authorization.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.authorization.dto.ClientConfiguration;
import org.sitmun.authorization.dto.ClientConfigurationForApplicationTerritory;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.authorization.service.ClientConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/workspace")
@Tag(name = "workspace", description = "workspace")
public class ClientConfigurationController {

  private final ClientConfigurationService clientConfigurationService;

  /**
   * Constructor.
   */
  public ClientConfigurationController(
    ClientConfigurationService clientConfigurationService) {
    this.clientConfigurationService = clientConfigurationService;
  }

  @GetMapping(path = "/application/{applicationId}/territory/{territoryId}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  public ResponseEntity<ClientConfigurationForApplicationTerritory> getDescription(
    @PathVariable("applicationId") Integer applicationId,
    @PathVariable("territoryId") Integer territoryId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return clientConfigurationService.describeFor(authentication.getName(), applicationId, territoryId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  @JsonView(ClientConfigurationViews.Base.class)
  public ResponseEntity<ClientConfiguration> getDescription() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return clientConfigurationService.describeFor(authentication.getName())
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
