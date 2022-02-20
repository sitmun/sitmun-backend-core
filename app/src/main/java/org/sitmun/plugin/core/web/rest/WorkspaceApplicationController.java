package org.sitmun.plugin.core.web.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.WorkspaceApplication;
import org.sitmun.plugin.core.security.SecurityUtils;
import org.sitmun.plugin.core.service.client.WorskpaceApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

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

  @SuppressWarnings("deprecation")
  @GetMapping(path = "/application/{applicationId}/territory/{territoryId}", produces = APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  @JsonView(WorkspaceApplication.View.class)
  public ResponseEntity<WorkspaceApplication> getDescription(
    @PathVariable("applicationId") Integer applicationId,
    @PathVariable("territoryId") Integer territoryId) {
    return SecurityUtils
      .getCurrentUserLogin()
      .flatMap((it) -> worskpaceApplicationService.describeFor(it, applicationId, territoryId))
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
