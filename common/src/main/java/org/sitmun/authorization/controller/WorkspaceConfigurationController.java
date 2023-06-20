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

/**
 * @deprecated Use {@link org.sitmun.authorization.controller.ClientConfigurationController} instead.
 */
@RestController
@RequestMapping("/api/workspace")
@Tag(name = "workspace", description = "workspace")
@Deprecated(forRemoval = true)
public class WorkspaceConfigurationController {

  private final ClientConfigurationService clientConfigurationService;

  /**
   * Constructor.
   */
  public WorkspaceConfigurationController(
    ClientConfigurationService clientConfigurationService) {
    this.clientConfigurationService = clientConfigurationService;
  }

  /**
   * @deprecated proof of concept
   */
  @GetMapping(path = "/application/{applicationId}/territory/{territoryId}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Deprecated(forRemoval = true)
  public ResponseEntity<ClientConfigurationForApplicationTerritory> getDescription(
    @PathVariable("applicationId") Integer applicationId,
    @PathVariable("territoryId") Integer territoryId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return clientConfigurationService.describeFor(authentication.getName(), applicationId, territoryId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * @deprecated proof of concept
   */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  @JsonView(ClientConfigurationViews.Base.class)
  @Deprecated(forRemoval = true)
  public ResponseEntity<ClientConfiguration> getDescription() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return clientConfigurationService.describeFor(authentication.getName())
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

}
