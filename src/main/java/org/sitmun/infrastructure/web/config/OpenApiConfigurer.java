package org.sitmun.infrastructure.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

/**
 * OpenAPI configuration for SITMUN API documentation.
 *
 * <p>This configuration is active only when the "openapi" profile is enabled. It provides: - API
 * information (title, version, description) - Security scheme configuration (JWT bearer
 * authentication) - Module-specific customization through properties
 */
@Configuration
@Profile("openapi")
public class OpenApiConfigurer {

  private final String moduleName;
  private final String apiVersion;

  public OpenApiConfigurer(
      @Value("${sitmun.module}") String moduleName, @Value("${sitmun.version}") String apiVersion) {
    this.moduleName = moduleName;
    this.apiVersion = apiVersion;
  }

  /**
   * Configures the OpenAPI documentation for SITMUN 3.0 API.
   *
   * <p>This configuration: - Sets up JWT bearer authentication as the security scheme - Customizes
   * the API title based on the module name - Includes version information - Adds a description
   * noting the active development status
   *
   * @return the OpenAPI configuration for SITMUN 3.0
   */
  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";
    final String apiTitle = "%s API".formatted(StringUtils.capitalize(moduleName));
    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .info(
            new Info()
                .title(apiTitle)
                .version(apiVersion)
                .description(
                    "Configuration manager of SITMUN applications."
                        + "<br/><br/>**Note**: In active development."));
  }
}
