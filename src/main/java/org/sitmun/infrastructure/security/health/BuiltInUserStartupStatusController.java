package org.sitmun.infrastructure.security.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only diagnostic for built-in user startup repair. Exposes only a stable state and
 * optional reason code — never secrets, hashes, or exception text.
 */
@RestController
public class BuiltInUserStartupStatusController {

  private final BuiltInUserStartupStatus status;

  public BuiltInUserStartupStatusController(BuiltInUserStartupStatus status) {
    this.status = status;
  }

  @GetMapping("/api/dashboard/startup")
  public StartupStatusResponse startup() {
    return switch (status.getState()) {
      case READY -> new StartupStatusResponse("ready", null);
      case BLOCKED -> new StartupStatusResponse("blocked", status.getReason());
      case INITIALIZING -> new StartupStatusResponse("initializing", null);
    };
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record StartupStatusResponse(String state, String reason) {}
}
