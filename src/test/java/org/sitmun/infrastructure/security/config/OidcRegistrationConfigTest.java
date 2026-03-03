package org.sitmun.infrastructure.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OidcRegistrationConfig unit tests")
class OidcRegistrationConfigTest {

  private OidcAuthenticationProperties oidcProperties;

  @BeforeEach
  void setUp() {
    oidcProperties = new OidcAuthenticationProperties();
  }

  private static OidcAuthenticationProperties.ProviderConfig minimalValidProvider(
      String registrationId) {
    OidcAuthenticationProperties.ProviderConfig config =
        new OidcAuthenticationProperties.ProviderConfig();
    config.setProviderName(registrationId);
    config.setClientId(registrationId + "-id");
    config.setIssuerUri("https://" + registrationId + ".example.com/oidc");
    config.setAuthorizationUri("https://" + registrationId + ".example.com/oidc/authorize");
    config.setTokenUri("https://" + registrationId + ".example.com/oidc/token");
    config.setUserInfoUri("https://" + registrationId + ".example.com/oidc/userinfo");
    config.setJwkSetUri("https://" + registrationId + ".example.com/oidc/jwks");
    config.setRedirectUri("http://localhost/login/oauth2/code/" + registrationId);
    config.setScope(List.of("openid", "profile", "email"));
    return config;
  }

  @Test
  @DisplayName("valid single provider produces correct ClientRegistration")
  void validSingleProvider_producesCorrectRegistration() {
    OidcAuthenticationProperties.ProviderConfig config = minimalValidProvider("mock");
    config.setUserNameAttributeName("preferred_username");
    oidcProperties.getProviders().put("mock", config);

    OidcRegistrationConfig configurer = new OidcRegistrationConfig(oidcProperties);
    ClientRegistrationRepository repo = configurer.clientRegistrationRepository();

    ClientRegistration reg = repo.findByRegistrationId("mock");
    assertThat(reg).isNotNull();
    assertThat(reg.getRegistrationId()).isEqualTo("mock");
    assertThat(reg.getClientId()).isEqualTo("mock-id");
    assertThat(reg.getAuthorizationGrantType())
        .isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
    assertThat(reg.getScopes()).containsExactlyInAnyOrder("openid", "profile", "email");
    assertThat(reg.getClientName()).isEqualTo("mock");
    assertThat(reg.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
        .isEqualTo("preferred_username");
  }

  @Test
  @DisplayName("provider with blank clientId is skipped")
  void providerWithBlankClientId_isSkipped() {
    OidcAuthenticationProperties.ProviderConfig config = minimalValidProvider("valid");
    oidcProperties.getProviders().put("valid", config);
    OidcAuthenticationProperties.ProviderConfig invalid = minimalValidProvider("invalid");
    invalid.setClientId("");
    oidcProperties.getProviders().put("invalid", invalid);

    OidcRegistrationConfig configurer = new OidcRegistrationConfig(oidcProperties);
    ClientRegistrationRepository repo = configurer.clientRegistrationRepository();

    assertThat(repo.findByRegistrationId("valid")).isNotNull();
    assertThat(repo.findByRegistrationId("invalid")).isNull();
  }

  @Test
  @DisplayName("provider with blank issuerUri is skipped")
  void providerWithBlankIssuerUri_isSkipped() {
    OidcAuthenticationProperties.ProviderConfig config = minimalValidProvider("valid");
    oidcProperties.getProviders().put("valid", config);
    OidcAuthenticationProperties.ProviderConfig invalid = minimalValidProvider("invalid");
    invalid.setIssuerUri("");
    oidcProperties.getProviders().put("invalid", invalid);

    OidcRegistrationConfig configurer = new OidcRegistrationConfig(oidcProperties);
    ClientRegistrationRepository repo = configurer.clientRegistrationRepository();

    assertThat(repo.findByRegistrationId("valid")).isNotNull();
    assertThat(repo.findByRegistrationId("invalid")).isNull();
  }

  @Test
  @DisplayName("optional clientSecret is set when present")
  void optionalClientSecret_present_isSet() {
    OidcAuthenticationProperties.ProviderConfig config = minimalValidProvider("mock");
    config.setClientSecret("secret");
    oidcProperties.getProviders().put("mock", config);

    OidcRegistrationConfig configurer = new OidcRegistrationConfig(oidcProperties);
    ClientRegistrationRepository repo = configurer.clientRegistrationRepository();

    ClientRegistration reg = repo.findByRegistrationId("mock");
    assertThat(reg).isNotNull();
    assertThat(reg.getClientSecret()).isEqualTo("secret");
  }

  @Test
  @DisplayName("optional clientSecret is null when blank")
  void optionalClientSecret_blank_isNull() {
    OidcAuthenticationProperties.ProviderConfig config = minimalValidProvider("mock");
    config.setClientSecret("");
    oidcProperties.getProviders().put("mock", config);

    OidcRegistrationConfig configurer = new OidcRegistrationConfig(oidcProperties);
    ClientRegistrationRepository repo = configurer.clientRegistrationRepository();

    ClientRegistration reg = repo.findByRegistrationId("mock");
    assertThat(reg).isNotNull();
    assertThat(StringUtils.hasText(reg.getClientSecret())).isFalse();
  }

  @Test
  @DisplayName("multiple valid providers are all registered")
  void multipleValidProviders_allRegistered() {
    oidcProperties.getProviders().put("one", minimalValidProvider("one"));
    oidcProperties.getProviders().put("two", minimalValidProvider("two"));

    OidcRegistrationConfig configurer = new OidcRegistrationConfig(oidcProperties);
    ClientRegistrationRepository repo = configurer.clientRegistrationRepository();

    assertThat(repo.findByRegistrationId("one")).isNotNull();
    assertThat(repo.findByRegistrationId("two")).isNotNull();
  }

  @Test
  @DisplayName("no valid providers throws IllegalStateException")
  void noValidProviders_throwsIllegalStateException() {
    OidcAuthenticationProperties.ProviderConfig invalid = minimalValidProvider("invalid");
    invalid.setClientId("");
    invalid.setIssuerUri("");
    oidcProperties.getProviders().put("invalid", invalid);

    OidcRegistrationConfig configurer = new OidcRegistrationConfig(oidcProperties);

    assertThatThrownBy(configurer::clientRegistrationRepository)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no OIDC providers were found");
  }
}
