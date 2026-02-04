package org.sitmun.authentication.oidc;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.service.OidcRedirectService;
import org.sitmun.infrastructure.security.filter.SitmunClientFilter;
import org.sitmun.test.AdditiveActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

@SpringBootTest
@AutoConfigureMockMvc
@AdditiveActiveProfiles(value = "oidc")
@DisplayName("OIDC integration tests")
class OidcLoginIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private OidcRedirectService redirectService;

  private static MockHttpServletRequest buildMockHttpServletRequestWithClientType(final String clientType) {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("client_type", clientType);
    return request;
  }

  private static void passRequestThroughFilter(
    final MockHttpServletRequest request,
    final MockHttpServletResponse response
  ) throws Exception {
    final SitmunClientFilter filter = new SitmunClientFilter();
    final FilterChain chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);
  }

  @Test
  void testOauthEndpointRedirectsToProviderAuthorization() throws Exception {
    mockMvc
      .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/oauth2/authorization/mock"))
      .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection())
      .andExpect(redirectedUrlPattern("https://mock.example.com/oidc/authorize?response_type=code&client_id=mock-id&scope=openid%20profile%20email&state=*&redirect_uri=https://test.example.com/login/oauth2/code/mock&nonce=*"));
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
