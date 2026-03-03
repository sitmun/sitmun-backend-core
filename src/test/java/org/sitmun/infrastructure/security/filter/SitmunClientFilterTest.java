package org.sitmun.infrastructure.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.OidcClientTypes;
import org.sitmun.authentication.service.OidcRedirectService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("SitmunClientFilter unit tests")
class SitmunClientFilterTest {

  private final SitmunClientFilter filter = new SitmunClientFilter();

  @Test
  @DisplayName("client_type param present: session created and attribute set")
  void clientTypeParamPresent_sessionCreatedAndAttributeSet() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(OidcClientTypes.QUERY_PARAM_NAME, OidcClientTypes.ADMIN);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    HttpSession session = request.getSession(false);
    assertThat(session).isNotNull();
    assertThat(session.getAttribute(OidcRedirectService.CLIENT_TYPE))
        .isEqualTo(OidcClientTypes.ADMIN);
    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("client_type param absent: no session created")
  void clientTypeParamAbsent_noSessionCreated() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(request.getSession(false)).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("chain.doFilter always called when param present")
  void chainDoFilterCalled_whenParamPresent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(OidcClientTypes.QUERY_PARAM_NAME, OidcClientTypes.VIEWER);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("existing session attribute overwritten when new client_type provided")
  void overwriteExistingAttribute_whenNewClientTypeProvided() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.getSession(true).setAttribute(OidcRedirectService.CLIENT_TYPE, OidcClientTypes.ADMIN);
    request.setParameter(OidcClientTypes.QUERY_PARAM_NAME, OidcClientTypes.VIEWER);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(request.getSession(false).getAttribute(OidcRedirectService.CLIENT_TYPE))
        .isEqualTo(OidcClientTypes.VIEWER);
    verify(chain).doFilter(request, response);
  }
}
