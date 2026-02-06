package org.sitmun.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.AuthProviderIds;
import org.sitmun.authentication.dto.AuthProviderDTO;
import org.sitmun.authentication.dto.OidcProviderDTO;
import org.sitmun.authentication.mapper.OidcProviderMapper;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.infrastructure.security.config.OidcAuthenticationProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationInfoController unit tests")
class AuthenticationInfoControllerTest {

  @Mock private Environment environment;

  @Mock private OidcAuthenticationProperties oidcProperties;

  @Mock private OidcProviderMapper oidcProviderMapper;

  private AuthenticationInfoController controller;

  @BeforeEach
  void setUp() {
    controller = new AuthenticationInfoController(oidcProperties, oidcProviderMapper, environment);
  }

  @Test
  @DisplayName("returns database only when no extra profiles")
  void returnsDatabaseOnly_whenNoExtraProfiles() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {Profiles.TEST});

    ResponseEntity<List<AuthProviderDTO>> response = controller.getProviders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<AuthProviderDTO> body = response.getBody();
    assertThat(body).isNotNull().hasSize(1);
    assertThat(body.get(0).id()).isEqualTo(AuthProviderIds.DATABASE);
    assertThat(body.get(0).providers()).isNull();
  }

  @Test
  @DisplayName("includes ldap when ldap profile active")
  void includesLdap_whenLdapProfileActive() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {Profiles.TEST, Profiles.LDAP});

    ResponseEntity<List<AuthProviderDTO>> response = controller.getProviders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<AuthProviderDTO> body = response.getBody();
    assertThat(body).isNotNull().hasSize(2);
    assertThat(body.get(0).id()).isEqualTo(AuthProviderIds.DATABASE);
    assertThat(body.get(1).id()).isEqualTo(Profiles.LDAP);
  }

  @Test
  @DisplayName("includes oidc with providers when oidc profile active")
  void includesOidcWithProviders_whenOidcProfileActive() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {Profiles.TEST, Profiles.OIDC});
    OidcAuthenticationProperties.ProviderConfig config =
        new OidcAuthenticationProperties.ProviderConfig();
    when(oidcProperties.getProviders()).thenReturn(Map.of("mock", config));
    when(oidcProviderMapper.toRepresentationDTO(any()))
        .thenReturn(new OidcProviderDTO("mock", "Mock OIDC", "https://example.com/img.png"));

    ResponseEntity<List<AuthProviderDTO>> response = controller.getProviders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<AuthProviderDTO> body = response.getBody();
    assertThat(body).isNotNull().hasSize(2);
    assertThat(body.get(0).id()).isEqualTo(AuthProviderIds.DATABASE);
    assertThat(body.get(1).id()).isEqualTo(Profiles.OIDC);
    assertThat(body.get(1).providers()).hasSize(1);
    assertThat(body.get(1).providers().get(0).providerName()).isEqualTo("mock");
    assertThat(body.get(1).providers().get(0).displayName()).isEqualTo("Mock OIDC");
    assertThat(body.get(1).providers().get(0).imagePath()).isEqualTo("https://example.com/img.png");
  }

  @Test
  @DisplayName("includes oidc with empty providers when oidc profile active but no providers")
  void includesOidcWithEmptyProviders_whenOidcProfileActiveButNoProviders() {
    when(environment.getActiveProfiles()).thenReturn(new String[] {Profiles.TEST, Profiles.OIDC});
    when(oidcProperties.getProviders()).thenReturn(Collections.emptyMap());

    ResponseEntity<List<AuthProviderDTO>> response = controller.getProviders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<AuthProviderDTO> body = response.getBody();
    assertThat(body).isNotNull().hasSize(2);
    assertThat(body.get(0).id()).isEqualTo(AuthProviderIds.DATABASE);
    assertThat(body.get(1).id()).isEqualTo(Profiles.OIDC);
    assertThat(body.get(1).providers()).isEmpty();
  }

  @Test
  @DisplayName("includes all three when ldap and oidc active")
  void includesAllThree_whenLdapAndOidcActive() {
    when(environment.getActiveProfiles())
        .thenReturn(new String[] {Profiles.TEST, Profiles.LDAP, Profiles.OIDC});
    OidcAuthenticationProperties.ProviderConfig config =
        new OidcAuthenticationProperties.ProviderConfig();
    when(oidcProperties.getProviders()).thenReturn(Map.of("mock", config));
    when(oidcProviderMapper.toRepresentationDTO(any()))
        .thenReturn(new OidcProviderDTO("mock", "Mock OIDC", "https://example.com/img.png"));

    ResponseEntity<List<AuthProviderDTO>> response = controller.getProviders();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<AuthProviderDTO> body = response.getBody();
    assertThat(body).isNotNull().hasSize(3);
    assertThat(body.get(0).id()).isEqualTo(AuthProviderIds.DATABASE);
    assertThat(body.get(1).id()).isEqualTo(Profiles.LDAP);
    assertThat(body.get(2).id()).isEqualTo(Profiles.OIDC);
    assertThat(body.get(2).providers()).hasSize(1);
  }
}
