package org.sitmun.authentication.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authentication.dto.AuthProviderDTO;
import org.sitmun.authentication.dto.OidcProviderDTO;
import org.sitmun.authentication.mapper.OidcProviderMapper;
import org.sitmun.infrastructure.security.config.OidcAuthenticationProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller to discover available authentication types and providers */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication discovery and providers")
public class AuthenticationInfoController {

  private final OidcAuthenticationProperties oidcProperties;
  private final OidcProviderMapper oidcProviderMapper;
  private final Environment environment;

  public AuthenticationInfoController(
      @Nullable OidcAuthenticationProperties oidcProperties,
      @Nullable OidcProviderMapper oidcProviderMapper,
      Environment environment) {
    this.oidcProperties = oidcProperties;
    this.oidcProviderMapper = oidcProviderMapper;
    this.environment = environment;
  }

  /**
   * @return List of available authentication providers
   */
  @GetMapping("/enabled-methods")
  @SecurityRequirements
  public ResponseEntity<List<AuthProviderDTO>> getProviders() {

    final List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
    final List<AuthProviderDTO> providers = new ArrayList<>();

    providers.add(AuthProviderDTO.builder().id("database").build());

    if (activeProfiles.contains("ldap")) {
      providers.add(AuthProviderDTO.builder().id("ldap").build());
    }

    if (activeProfiles.contains("oidc")) {
      final List<OidcProviderDTO> oidcProviders =
          oidcProperties.getProviders().values().stream()
              .map(oidcProviderMapper::toRepresentationDTO)
              .toList();

      providers.add(AuthProviderDTO.builder().id("oidc").providers(oidcProviders).build());
    }

    return ResponseEntity.ok(providers);
  }
}
