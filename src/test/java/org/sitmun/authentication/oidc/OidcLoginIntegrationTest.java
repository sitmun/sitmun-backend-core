package org.sitmun.authentication.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.service.OidcRedirectService;
import org.sitmun.infrastructure.security.filter.SitmunClientFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "h2", "oidc"})
@DisplayName("OIDC integration tests")
class OidcLoginIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private OidcRedirectService redirectService;

  private static MockHttpServletRequest buildMockHttpServletRequestWithClientType(
      final String clientType) {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("client_type", clientType);
    return request;
  }

  private static void passRequestThroughFilter(
      final MockHttpServletRequest request, final MockHttpServletResponse response)
      throws Exception {
    final SitmunClientFilter filter = new SitmunClientFilter();
    final FilterChain chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);
  }

  @Test
  @DisplayName("OAuth2 authorization endpoint redirects to provider authorization URL")
  void testOauthEndpointRedirectsToProviderAuthorization() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/oauth2/authorization/mock"))
        .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
        .andExpect(
            redirectedUrlPattern(
                "https://mock.example.com/oidc/authorize?response_type=code&client_id=mock-id&scope=openid%20profile%20email&state=*&redirect_uri=http://localhost/login/oauth2/code/mock&nonce=*"));
  }

  @Test
  @DisplayName("OAuth2 authorization endpoint response includes required info")
  void testAuthorizationEndpointResponseIncludesRequiredInfo() throws Exception {
    final MvcResult result =
        mockMvc
            .perform(get("/oauth2/authorization/mock"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

    final String redirectUrl = result.getResponse().getHeader("Location");
    assertThat(redirectUrl).contains("state=");
    assertThat(redirectUrl).contains("nonce=");
    assertThat(redirectUrl).contains("response_type=code");
    assertThat(redirectUrl).contains("client_id=mock-id");
  }

  @Test
  @DisplayName("Client type parameter is stored in session")
  void testClientTypeParameterStoredInSession() throws Exception {
    final MvcResult result =
        mockMvc
            .perform(get("/oauth2/authorization/mock").param("client_type", "admin"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

    final HttpSession session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    assertThat(session.getAttribute(OidcRedirectService.CLIENT_TYPE)).isEqualTo("admin");
  }

  @Test
  void testOidcRedirectServiceReturnsAdminUrlForAdminClientType() throws Exception {
    final MockHttpServletRequest request = buildMockHttpServletRequestWithClientType("admin");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    passRequestThroughFilter(request, response);
    final String redirectUrl = redirectService.selectRedirectUrl(request);
    assertThat(redirectUrl).isEqualTo("http://localhost:4200/#/callback");
  }

  @Test
  void testOidcRedirectServiceReturnsViewerUrlForViewerClientType() throws Exception {
    final MockHttpServletRequest request = buildMockHttpServletRequestWithClientType("viewer");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    passRequestThroughFilter(request, response);
    final String redirectUrl = redirectService.selectRedirectUrl(request);
    assertThat(redirectUrl).isEqualTo("http://localhost:4201/callback");
  }

  @Test
  void testOidcRedirectServiceReturnsDefaultUrlForNoClientType() throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    final MockHttpServletResponse response = new MockHttpServletResponse();
    passRequestThroughFilter(request, response);
    final String redirectUrl = redirectService.selectRedirectUrl(request);
    assertThat(redirectUrl).isEqualTo("http://localhost:4200/default");
  }
}
