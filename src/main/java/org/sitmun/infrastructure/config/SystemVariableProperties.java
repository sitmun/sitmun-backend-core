package org.sitmun.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for SITMUN system variables. System variables are backend-only variables
 * resolved using SpEL expressions. They are defined in application.yml under
 * sitmun.variables.system.
 */
@Configuration
@ConfigurationProperties(prefix = "sitmun.variables")
@Data
public class SystemVariableProperties {

  /**
   * Map of system variable names to their SpEL expression definitions. Example: USER_ID ->
   * #{user.id} These variables are resolved at runtime from the request context (User, Territory,
   * Application entities).
   */
  private Map<String, String> system = new HashMap<>();
}
