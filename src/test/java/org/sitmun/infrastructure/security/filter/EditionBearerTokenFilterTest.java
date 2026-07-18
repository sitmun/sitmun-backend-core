package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.sitmun.infrastructure.security.jwt.MobileTokenScopes;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EditionBearerTokenFilter")
class EditionBearerTokenFilterTest {

  @Mock JsonWebTokenService jsonWebTokenService;
  @Mock UserDetailsService userDetailsService;
  @Mock UserRepository userRepository;
  @Mock Rfc9457ResponseWriter responseWriter;
  @Mock FilterChain filterChain;
  @Mock Claims claims;
  @Mock UserDetails userDetails;

  private EditionBearerTokenFilter filter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    filter =
        new EditionBearerTokenFilter(
            jsonWebTokenService, userDetailsService, userRepository, responseWriter);
  }

  @Test
  @DisplayName("Absent Authorization continues the chain")
  void absentAuthorizationContinues() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(responseWriter, never()).writeUnauthorized(any(), any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("Valid edition access token sets MOBILE_EDITION and SCOPE authorities")
  void validTokenSetsMobileAuthorities() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer mobile-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    Date passwordChange = new Date();

    when(jsonWebTokenService.parseClaims("mobile-token")).thenReturn(claims);
    when(jsonWebTokenService.isEditionAccessToken(claims)).thenReturn(true);
    when(claims.getSubject()).thenReturn("edition-user");
    when(userDetailsService.loadUserByUsername("edition-user")).thenReturn(userDetails);
    when(userDetails.isAccountNonLocked()).thenReturn(true);
    User user = new User();
    user.setUsername("edition-user");
    user.setLastPasswordChange(passwordChange);
    when(userRepository.findByUsername("edition-user")).thenReturn(Optional.of(user));
    when(jsonWebTokenService.validateEditionAccessToken("mobile-token", passwordChange))
        .thenReturn(true);
    when(jsonWebTokenService.getScopes(claims)).thenReturn(MobileTokenScopes.EDITION_ACCESS);

    filter.doFilter(request, response, filterChain);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isNotNull();
    assertThat(authentication.getName()).isEqualTo("edition-user");
    assertThat(authentication.getAuthorities())
        .extracting(Object::toString)
        .contains(
            SecurityRole.MOBILE_EDITION.authority(),
            MobileTokenScopes.springAuthority(MobileTokenScopes.CLIENT_APPLICATIONS_READ),
            MobileTokenScopes.springAuthority(MobileTokenScopes.PROXY_TOKEN_ISSUE));
    assertThat(authentication.getAuthorities())
        .extracting(Object::toString)
        .doesNotContain("ROLE_USER", "ROLE_ADMIN");
    verify(filterChain).doFilter(request, response);
    verify(responseWriter, never()).writeUnauthorized(any(), any());
  }

  @Test
  @DisplayName("Wrong token use returns 401 without clearing cookies")
  void wrongTokenUseReturnsUnauthorized() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer other-token");
    request.setCookies(new jakarta.servlet.http.Cookie("viewer_access_token", "cookie-value"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(jsonWebTokenService.parseClaims("other-token")).thenReturn(claims);
    when(jsonWebTokenService.isEditionAccessToken(claims)).thenReturn(false);

    filter.doFilter(request, response, filterChain);

    verify(responseWriter).writeUnauthorized(request, response);
    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getCookies()).isEmpty();
  }

  @Test
  @DisplayName("Expired Bearer returns 401")
  void expiredTokenReturnsUnauthorized() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired");
    MockHttpServletResponse response = new MockHttpServletResponse();

    when(jsonWebTokenService.parseClaims("expired"))
        .thenThrow(new ExpiredJwtException(null, null, "expired"));

    filter.doFilter(request, response, filterChain);

    verify(responseWriter).writeUnauthorized(request, response);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  @DisplayName("Proxy principal skips Bearer processing")
  void proxyPrincipalSkipsBearer() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "middleware",
                null,
                List.of(new SimpleGrantedAuthority(SecurityRole.PROXY.authority()))));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer proxy-delegated");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jsonWebTokenService, never()).parseClaims(any());
  }
}
