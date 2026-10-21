package org.sitmun.authorization.proxy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator.Outcome;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProxyDelegatedTokenAuthenticator")
class ProxyDelegatedTokenAuthenticatorTest {

  private static final String TOKEN = "legacy-token";
  private static final Date PASSWORD_CHANGE = new Date(1_700_000_000_000L);

  @Mock private JsonWebTokenService jsonWebTokenService;
  @Mock private UserRepository userRepository;
  @Mock private UserApplicationAccessPolicy userApplicationAccessPolicy;

  private ProxyDelegatedTokenAuthenticator authenticator;

  @BeforeEach
  void setUp() {
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn("admin");
    User user = mock(User.class);
    when(user.getLastPasswordChange()).thenReturn(PASSWORD_CHANGE);
    when(jsonWebTokenService.parseClaims(TOKEN)).thenReturn(claims);
    when(jsonWebTokenService.isEditionAccessToken(claims)).thenReturn(false);
    when(jsonWebTokenService.isMobileProxyAccessToken(claims)).thenReturn(false);
    when(jsonWebTokenService.isLegacyProxyToken(claims)).thenReturn(true);
    when(userApplicationAccessPolicy.isBlockedAccount("admin")).thenReturn(false);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
    lenient().when(jsonWebTokenService.validateLegacyProxyToken(TOKEN)).thenReturn(true);

    authenticator =
        new ProxyDelegatedTokenAuthenticator(
            jsonWebTokenService, userRepository, userApplicationAccessPolicy);
  }

  @Test
  @DisplayName("legacy proxy token is denied when the password change does not match")
  void legacyTokenDeniedWhenPasswordChangeDoesNotMatch() {
    when(jsonWebTokenService.validateLegacyProxyToken(TOKEN, PASSWORD_CHANGE)).thenReturn(false);

    assertThat(authenticator.authenticate(TOKEN, List.of())).isInstanceOf(Outcome.Denied.class);
  }
}
