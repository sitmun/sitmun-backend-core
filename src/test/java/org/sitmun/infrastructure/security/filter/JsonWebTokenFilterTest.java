package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonWebTokenFilter")
class JsonWebTokenFilterTest {

  @Mock private JsonWebTokenService jsonWebTokenService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private UserRepository userRepository;
  @Mock private jakarta.servlet.FilterChain filterChain;

  private JsonWebTokenFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JsonWebTokenFilter(userDetailsService, jsonWebTokenService, userRepository);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("no cookies: continues chain without authentication")
  void noCookies_continuesWithoutAuth() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("no access_token cookie: continues chain without authentication")
  void noAccessTokenCookie_continuesWithoutAuth() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("other", "x"));
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(jsonWebTokenService, never()).getUsernameFromToken(any());
  }

  @Test
  @DisplayName("valid token: sets authentication and continues chain")
  void validToken_setsAuthentication() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "jwt"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(jsonWebTokenService.getUsernameFromToken("jwt")).thenReturn("admin");
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.builder()
            .username("admin")
            .password("p")
            .authorities(new SimpleGrantedAuthority(SecurityRole.ADMIN.authority()))
            .build();
    when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
    org.sitmun.domain.user.User user =
        org.sitmun.domain.user.User.builder()
            .username("admin")
            .administrator(true)
            .lastPasswordChange(new Date())
            .build();
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
    when(jsonWebTokenService.validateToken(eq("jwt"), eq(userDetails), any(Date.class)))
        .thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
  }

  @Test
  @DisplayName("unknown user: continues chain without authentication")
  void unknownUser_continuesWithoutAuth() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "jwt"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(jsonWebTokenService.getUsernameFromToken("jwt")).thenReturn("ghost");
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.builder()
            .username("ghost")
            .password("p")
            .authorities(new SimpleGrantedAuthority(SecurityRole.USER.authority()))
            .build();
    when(userDetailsService.loadUserByUsername("ghost")).thenReturn(userDetails);
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("expired token: continues chain without authentication")
  void expiredToken_continuesWithoutAuth() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "exp"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(jsonWebTokenService.getUsernameFromToken("exp"))
        .thenThrow(new ExpiredJwtException(null, null, "expired"));

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("existing security context: does not load user details")
  void existingAuthentication_skipsLoadingUserDetails() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "session",
                null,
                java.util.List.of(new SimpleGrantedAuthority(SecurityRole.USER.authority()))));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "jwt"));
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(jsonWebTokenService.getUsernameFromToken("jwt")).thenReturn("admin");

    filter.doFilterInternal(request, response, filterChain);

    verify(userDetailsService, never()).loadUserByUsername(any());
    verify(filterChain).doFilter(request, response);
  }
}
