package org.sitmun.web.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Workspace;
import org.sitmun.security.SecurityUtils;
import org.sitmun.service.client.WorskpaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@RestController
@RequestMapping("/api/workspace")
@Tag(name = "workspace", description = "workspace")
public class WorkspaceController {

  private final WorskpaceService worskpaceService;

  /**
   * Constructor.
   */
  public WorkspaceController(
    WorskpaceService worskpaceService) {
    this.worskpaceService = worskpaceService;
  }

  @SuppressWarnings("deprecation")
  @GetMapping(produces = APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  @JsonView(Workspace.View.class)
  public ResponseEntity<Workspace> getDescription() {
    return SecurityUtils
      .getCurrentUserLogin()
      .flatMap(worskpaceService::describeFor)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
