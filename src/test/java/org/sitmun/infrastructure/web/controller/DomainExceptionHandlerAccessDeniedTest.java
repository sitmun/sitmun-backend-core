package org.sitmun.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainExceptionHandler — AccessDenied")
class DomainExceptionHandlerAccessDeniedTest {

  private static final String BLOCKED_USER = "blocked-user";

  @Mock private MessageSource messageSource;
  @Mock private CookieService cookieService;
  @Mock private UserApplicationAccessPolicy userApplicationAccessPolicy;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private DomainExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DomainExceptionHandler(messageSource, cookieService, userApplicationAccessPolicy);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("clears access_token cookie on 403 when authenticated principal is blocked")
  void clearsCookieForBlockedAuthenticatedPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                BLOCKED_USER, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    when(userApplicationAccessPolicy.isBlockedAccount(BLOCKED_USER)).thenReturn(true);

    var result =
        handler.handleAccessDeniedException(
            new AccessDeniedException("Access denied: user account is blocked"), request, response);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(cookieService).clearAccessTokenCookie(request, response);
  }

  @Test
  @DisplayName("does not clear cookie on 403 when authenticated principal is not blocked")
  void doesNotClearCookieForActiveAuthenticatedPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "active-user", null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    when(userApplicationAccessPolicy.isBlockedAccount("active-user")).thenReturn(false);

    var result =
        handler.handleAccessDeniedException(
            new AccessDeniedException("Access denied to application"), request, response);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    verify(cookieService, never()).clearAccessTokenCookie(request, response);
  }

  @Test
  @DisplayName("does not clear cookie on 401 for anonymous principal")
  void doesNotClearCookieForAnonymousPrincipal() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "anonymous", "public", AuthorityUtils.createAuthorityList("ROLE_PUBLIC")));

    var result =
        handler.handleAccessDeniedException(
            new AccessDeniedException("Access denied: user account is blocked"), request, response);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(cookieService, never()).clearAccessTokenCookie(request, response);
    verify(userApplicationAccessPolicy, never())
        .isBlockedAccount(org.mockito.ArgumentMatchers.any());
  }
}
