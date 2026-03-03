package org.sitmun.infrastructure.security.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Configuration properties for OIDC authentication. */
@Getter
@Setter
@Profile(Profiles.OIDC)
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

    /**
     * OIDC claim used as the principal name. Default {@code "sub"} (subject), the standard unique
     * user id. Override if your IdP uses another claim (e.g. {@code preferred_username}).
     */
    private String userNameAttributeName = "sub";

    /**
     * OAuth2 scopes requested. Default: {@code openid} (required for OIDC), {@code profile}, {@code
     * email}. Override per provider if needed.
     */
    private List<String> scope = List.of("openid", "profile", "email");
  }
}
