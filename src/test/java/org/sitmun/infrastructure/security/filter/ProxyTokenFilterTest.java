package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.sitmun.infrastructure.security.core.SecurityConstants.PROXY_MIDDLEWARE_KEY;
import static org.sitmun.infrastructure.security.core.SecurityConstants.PROXY_MIDDLEWARE_PRINCIPAL;
import static org.sitmun.infrastructure.security.core.SecurityRole.createAuthorityList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProxyTokenFilter")
class ProxyTokenFilterTest {

  /**
   * Arbitrary shared secret for unit tests (production value is configured, not a code constant).
   */
  private static final String TEST_PROXY_SHARED_SECRET = "shared-secret";

  @Mock private jakarta.servlet.FilterChain filterChain;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("matching header value: sets service authentication")
  void matchingSecret_setsAuthentication() throws Exception {
    ProxyTokenFilter filter =
        new ProxyTokenFilter(
            PROXY_MIDDLEWARE_KEY,
            PROXY_MIDDLEWARE_PRINCIPAL,
            createAuthorityList(SecurityRole.PROXY),
            TEST_PROXY_SHARED_SECRET);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(PROXY_MIDDLEWARE_KEY, TEST_PROXY_SHARED_SECRET);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo(PROXY_MIDDLEWARE_PRINCIPAL);
    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
        .extracting("authority")
        .containsExactly(SecurityRole.PROXY.authority());
  }

  @Test
  @DisplayName("wrong header value: no authentication")
  void wrongSecret_noAuthentication() throws Exception {
    ProxyTokenFilter filter =
        new ProxyTokenFilter(
            PROXY_MIDDLEWARE_KEY,
            PROXY_MIDDLEWARE_PRINCIPAL,
            createAuthorityList(SecurityRole.PROXY),
            TEST_PROXY_SHARED_SECRET);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(PROXY_MIDDLEWARE_KEY, "wrong");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("missing header: no authentication")
  void missingHeader_noAuthentication() throws Exception {
    ProxyTokenFilter filter =
        new ProxyTokenFilter(
            PROXY_MIDDLEWARE_KEY,
            PROXY_MIDDLEWARE_PRINCIPAL,
            createAuthorityList(SecurityRole.PROXY),
            TEST_PROXY_SHARED_SECRET);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }
}
