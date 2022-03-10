package org.sitmun.feature.client.config;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/workspace")
@Tag(name = "workspace", description = "workspace")
public class WorkspaceApplicationController {

  private final WorskpaceApplicationService worskpaceApplicationService;

  /**
   * Constructor.
   */
  public WorkspaceApplicationController(
    WorskpaceApplicationService worskpaceApplicationService) {
    this.worskpaceApplicationService = worskpaceApplicationService;
  }

  @GetMapping(path = "/application/{applicationId}/territory/{territoryId}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  @JsonView(Views.WorkspaceApplication.class)
  public ResponseEntity<WorkspaceApplication> getDescription(
    @PathVariable("applicationId") Integer applicationId,
    @PathVariable("territoryId") Integer territoryId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return worskpaceApplicationService.describeFor(authentication.getName(), applicationId, territoryId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
