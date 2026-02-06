package org.sitmun.authentication.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.handler.OidcAuthenticationSuccessHandler;
import org.sitmun.authentication.service.OidcRedirectService;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
@DisplayName("OIDC unit tests")
class OidcLoginUnitTest {

  @Mock private OidcRedirectService redirectService;

  @Mock private UserRepository userRepository;

  @Mock private UserDetailsService userDetailsService;

  @Mock private JsonWebTokenService jsonWebTokenService;

  @InjectMocks private OidcAuthenticationSuccessHandler successHandler;

  @Test
  @DisplayName("Cookie is set on successful OIDC authentication")
  void testCookieSetOnSuccessfulAuthentication() throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getPreferredUsername()).thenReturn("jdoe");

    when(userRepository.findByUsername(any()))
        .thenReturn(
            java.util.Optional.of(
                User.builder()
                    .id(1)
                    .username("jdoe")
                    .password("password")
                    .firstName("John")
                    .lastName("Doe")
                    .identificationNumber("123456789")
                    .identificationType("1")
                    .administrator(false)
                    .email("jdoe@test.com")
                    .createdDate(new Date())
                    .lastModifiedDate(new Date())
                    .build()));

    when(jsonWebTokenService.generateToken(ArgumentMatchers.<UserDetails>any(), any()))
        .thenReturn("mock-jwt-token");

    final Authentication auth =
        new OAuth2AuthenticationToken(oidcUser, java.util.List.of(), "mock");
    when(redirectService.selectRedirectUrl(any())).thenReturn("/success");

    final Field field =
        OidcAuthenticationSuccessHandler.class.getDeclaredField("oidcCookieHttpOnly");
    field.setAccessible(true);
    field.set(successHandler, Boolean.TRUE);

    successHandler.onAuthenticationSuccess(request, response, auth);
    Arrays.stream(response.getCookies())
        .filter(cookie -> "oidc_token".equals(cookie.getName()))
        .findFirst()
        .ifPresent(
            cookie -> {
              assertThat(cookie.getValue()).isEqualTo("mock-jwt-token");
              assertThat(cookie.isHttpOnly()).isTrue();
            });
  }
}
