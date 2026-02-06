package org.sitmun.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.OidcClientTypes;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("OidcRedirectService unit tests")
class OidcRedirectServiceTest {

  private static final String DEFAULT_URL = "http://localhost:9000/default";
  private static final String ADMIN_URL = "http://localhost:4200/#/callback";
  private static final String VIEWER_URL = "http://localhost:4201/callback";

  private OidcRedirectService service;

  @BeforeEach
  void setUp() {
    service = new OidcRedirectService();
    ReflectionTestUtils.setField(service, "defaultUrl", DEFAULT_URL);
    ReflectionTestUtils.setField(service, "adminUrl", ADMIN_URL);
    ReflectionTestUtils.setField(service, "viewerUrl", VIEWER_URL);
  }

  @Test
  @DisplayName("selectRedirectUrl returns admin URL when session attribute is ADMIN")
  void selectRedirectUrl_returnsAdminUrl_whenSessionAttributeIsAdmin() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    HttpSession session = request.getSession(true);
    session.setAttribute(OidcRedirectService.CLIENT_TYPE, OidcClientTypes.ADMIN);

    String result = service.selectRedirectUrl(request);

    assertThat(result).isEqualTo(ADMIN_URL);
  }

  @Test
  @DisplayName("selectRedirectUrl returns viewer URL when session attribute is VIEWER")
  void selectRedirectUrl_returnsViewerUrl_whenSessionAttributeIsViewer() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    HttpSession session = request.getSession(true);
    session.setAttribute(OidcRedirectService.CLIENT_TYPE, OidcClientTypes.VIEWER);

    String result = service.selectRedirectUrl(request);

    assertThat(result).isEqualTo(VIEWER_URL);
  }

  @Test
  @DisplayName("selectRedirectUrl returns default URL when no session exists")
  void selectRedirectUrl_returnsDefaultUrl_whenNoSession() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    // getSession(false) without prior getSession(true) yields null

    String result = service.selectRedirectUrl(request);

    assertThat(result).isEqualTo(DEFAULT_URL);
  }

  @Test
  @DisplayName(
      "selectRedirectUrl returns default URL when session exists but CLIENT_TYPE attribute is null")
  void selectRedirectUrl_returnsDefaultUrl_whenSessionAttributeIsNull() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.getSession(true);
    // CLIENT_TYPE attribute never set -> getAttribute returns null

    String result = service.selectRedirectUrl(request);

    assertThat(result).isEqualTo(DEFAULT_URL);
  }

  @Test
  @DisplayName("selectRedirectUrl returns default URL when session has empty string as client type")
  void selectRedirectUrl_returnsDefaultUrl_whenSessionAttributeIsEmpty() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    HttpSession session = request.getSession(true);
    session.setAttribute(OidcRedirectService.CLIENT_TYPE, "");

    String result = service.selectRedirectUrl(request);

    assertThat(result).isEqualTo(DEFAULT_URL);
  }

  @Test
  @DisplayName("selectRedirectUrl returns default URL when session has unknown client type")
  void selectRedirectUrl_returnsDefaultUrl_whenSessionAttributeIsUnknown() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    HttpSession session = request.getSession(true);
    session.setAttribute(OidcRedirectService.CLIENT_TYPE, "other");

    String result = service.selectRedirectUrl(request);

    assertThat(result).isEqualTo(DEFAULT_URL);
  }
}
