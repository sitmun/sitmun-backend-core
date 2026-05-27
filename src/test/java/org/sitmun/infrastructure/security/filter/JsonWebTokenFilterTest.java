package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonWebTokenFilter")
class JsonWebTokenFilterTest {

  @Mock private UserDetailsService userDetailsService;
  @Mock private JsonWebTokenService jsonWebTokenService;
  @Mock private UserRepository userRepository;
  @Mock private FilterChain filterChain;

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
  @DisplayName("Blocked user's valid JWT is rejected: security context stays empty")
  void blockedUserJwtIsRejected() throws Exception {
    UserDetails blockedDetails = mock(UserDetails.class);
    when(blockedDetails.isAccountNonLocked()).thenReturn(false);

    when(jsonWebTokenService.getUsernameFromToken(anyString())).thenReturn("blocked");
    when(userDetailsService.loadUserByUsername("blocked")).thenReturn(blockedDetails);

    MockHttpServletRequest request = new MockHttpServletRequest();
    jakarta.servlet.http.Cookie cookie =
        new jakarta.servlet.http.Cookie(
            AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "some.valid.token");
    request.setCookies(cookie);

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Non-locked user's valid JWT is authenticated")
  void nonLockedUserJwtIsAuthenticated() throws Exception {
    UserDetails activeDetails = mock(UserDetails.class);
    when(activeDetails.isAccountNonLocked()).thenReturn(true);
    when(activeDetails.getAuthorities()).thenReturn(List.of());

    User user = User.builder().username("active").blocked(false).build();

    when(jsonWebTokenService.getUsernameFromToken(anyString())).thenReturn("active");
    when(userDetailsService.loadUserByUsername("active")).thenReturn(activeDetails);
    when(userRepository.findByUsername("active")).thenReturn(Optional.of(user));
    when(jsonWebTokenService.validateToken(anyString(), any(), any())).thenReturn(true);

    MockHttpServletRequest request = new MockHttpServletRequest();
    jakarta.servlet.http.Cookie cookie =
        new jakarta.servlet.http.Cookie(
            AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, "some.valid.token");
    request.setCookies(cookie);

    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
  }
}
