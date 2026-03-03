package org.sitmun.infrastructure.security.config;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@Profile(Profiles.OIDC)
@RequiredArgsConstructor
public class OidcRegistrationConfig {

  private final OidcAuthenticationProperties oidcProperties;

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    final List<ClientRegistration> registrations = new ArrayList<>();

    oidcProperties
        .getProviders()
        .forEach(
            (providerId, config) -> {
              if (StringUtils.hasText(config.getClientId())
                  && StringUtils.hasText(config.getIssuerUri())) {

                final ClientRegistration.Builder builder =
                    ClientRegistration.withRegistrationId(providerId)
                        .clientId(config.getClientId())
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationUri(config.getAuthorizationUri())
                        .tokenUri(config.getTokenUri())
                        .userInfoUri(config.getUserInfoUri())
                        .userNameAttributeName(config.getUserNameAttributeName())
                        .jwkSetUri(config.getJwkSetUri())
                        .redirectUri(config.getRedirectUri())
                        .scope(config.getScope())
                        .clientName(config.getProviderName());

                if (StringUtils.hasText(config.getClientSecret())) {
                  builder.clientSecret(config.getClientSecret());
                }

                registrations.add(builder.build());
              }
            });

    if (registrations.isEmpty()) {
      throw new IllegalStateException(
          "OIDC profile is enabled, but no OIDC providers were found. Set SITMUN_AUTHENTICATION_OIDC_PROVIDERS_* environment variables.");
    }

    return new InMemoryClientRegistrationRepository(registrations);
  }
}
