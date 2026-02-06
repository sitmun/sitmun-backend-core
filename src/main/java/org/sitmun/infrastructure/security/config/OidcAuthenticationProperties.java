package org.sitmun.infrastructure.security.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for OIDC authentication.
 */
@Getter
@Setter
@Profile("oidc")
@Configuration
@ConfigurationProperties(prefix = "sitmun.authentication.oidc")
public class OidcAuthenticationProperties {

  private Map<String, ProviderConfig> providers = new HashMap<>();

  @Data
  public static class ProviderConfig {
    private String providerName;
    private String clientId;
    private String clientSecret;
    private String issuerUri;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String jwkSetUri;
    private String redirectUri;
    private String imagePath;
    private String displayName;
    private String userNameAttributeName = "sub";
    private List<String> scope = List.of("openid", "profile", "email");
  }
}
