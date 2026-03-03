package org.sitmun.authentication.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authentication.service.OidcRedirectService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OidcAuthenticationFailureHandler unit tests")
class OidcAuthenticationFailureHandlerTest {

  private static final String ADMIN_URL = "http://localhost:4200/#/callback";
  private static final String VIEWER_URL = "http://localhost:4201/callback";
  private static final String DEFAULT_URL = "http://localhost:4200/default";

  @Mock private OidcRedirectService redirectService;

  private OidcAuthenticationFailureHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OidcAuthenticationFailureHandler(redirectService);
  }

  @Test
  @DisplayName("redirects to admin URL when redirect service returns admin URL")
  void redirectsToAdminUrl() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(ADMIN_URL);

    handler.onAuthenticationFailure(request, response, new AuthenticationServiceException("test"));

    assertThat(response.getRedirectedUrl()).isEqualTo(ADMIN_URL);
  }

  @Test
  @DisplayName("redirects to viewer URL when redirect service returns viewer URL")
  void redirectsToViewerUrl() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(VIEWER_URL);

    handler.onAuthenticationFailure(request, response, new AuthenticationServiceException("test"));

    assertThat(response.getRedirectedUrl()).isEqualTo(VIEWER_URL);
  }

  @Test
  @DisplayName("redirects to default URL when redirect service returns default URL")
  void redirectsToDefaultUrl() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(redirectService.selectRedirectUrl(request)).thenReturn(DEFAULT_URL);

    handler.onAuthenticationFailure(request, response, new AuthenticationServiceException("test"));

    assertThat(response.getRedirectedUrl()).isEqualTo(DEFAULT_URL);
  }
}
