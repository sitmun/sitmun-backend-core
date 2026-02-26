package org.sitmun.infrastructure.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system configuration endpoints. Provides metadata and configuration
 * information for admin UI.
 */
@RestController
@RequestMapping("/api/config/system")
@RequiredArgsConstructor
public class SystemConfigurationController {

  private final SystemVariableResolver systemVariableResolver;

  /**
   * Returns all available system variables that can be used in #{...} expressions. This endpoint is
   * intended for admin UI to provide autocomplete and documentation.
   *
   * @return Map of variable names to their SpEL expressions
   */
  @GetMapping("/variables")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, String>> getSystemVariables() {
    return ResponseEntity.ok(systemVariableResolver.getAvailableVariables());
  }
}
