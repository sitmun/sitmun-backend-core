package org.sitmun.authorization.proxy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.jsonwebtoken.ExpiredJwtException;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.controllers.ProxyConfigurationController;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProxyConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProxyConfigurationControllerErrorTest {

  private static final String REQUEST =
      """
      {
        "appId": 1,
        "terId": 1,
        "type": "SQL",
        "typeId": 23,
        "method": "GET",
        "id_token": "proxy-jwt"
      }
      """;

  @Autowired private MockMvc mvc;

  @MockitoBean private ProxyConfigurationService proxyConfigurationService;

  @MockitoBean private JsonWebTokenService jsonWebTokenService;

  @MockitoBean private TranslationRepository translationRepository;

  @MockitoBean private RequestLocaleResolutionService requestLocaleResolutionService;

  @MockitoBean private CookieService cookieService;

  @MockitoBean private UserApplicationAccessPolicy userApplicationAccessPolicy;

  @Test
  void returnsUnauthorizedProblemForInvalidProxyJwt() throws Exception {
    when(jsonWebTokenService.getUsernameFromToken("proxy-jwt"))
        .thenThrow(new IllegalArgumentException("invalid JWT"));

    mvc.perform(post("/api/config/proxy").contentType(APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("https://sitmun.org/problems/unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.instance").value("/api/config/proxy"));
  }

  @Test
  void returnsUnauthorizedProblemForExpiredProxyJwt() throws Exception {
    when(jsonWebTokenService.getUsernameFromToken("proxy-jwt"))
        .thenThrow(new ExpiredJwtException(null, null, "expired JWT", null));

    mvc.perform(post("/api/config/proxy").contentType(APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void doesNotClassifyUnexpectedJwtServiceFailureAsUnauthorized() throws Exception {
    when(jsonWebTokenService.getUsernameFromToken("proxy-jwt"))
        .thenThrow(new IllegalStateException("unexpected failure"));

    mvc.perform(post("/api/config/proxy").contentType(APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(500));
  }

  @Test
  void returnsForbiddenProblemWhenIdentifiedPrincipalCannotAccessResource() throws Exception {
    when(jsonWebTokenService.getUsernameFromToken("proxy-jwt")).thenReturn("alice");
    when(jsonWebTokenService.getExpirationDateFromToken("proxy-jwt")).thenReturn(new Date());
    when(proxyConfigurationService.validateUserAccess(any(), eq("alice"))).thenReturn(false);

    mvc.perform(post("/api/config/proxy").contentType(APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("https://sitmun.org/problems/forbidden"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.title").value("Forbidden"))
        .andExpect(jsonPath("$.instance").value("/api/config/proxy"));
  }

  @Test
  void returnsUnauthorizedProblemWhenProxyJwtIdentifiesBlockedAccount() throws Exception {
    when(jsonWebTokenService.getUsernameFromToken("proxy-jwt")).thenReturn("alice");
    when(jsonWebTokenService.getExpirationDateFromToken("proxy-jwt")).thenReturn(new Date());
    when(userApplicationAccessPolicy.isBlockedAccount("alice")).thenReturn(true);

    mvc.perform(post("/api/config/proxy").contentType(APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("https://sitmun.org/problems/unauthorized"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Authentication is required"))
        .andExpect(jsonPath("$.instance").value("/api/config/proxy"));
  }

  @Test
  void returnsBadRequestProblemForInvalidProxyConfiguration() throws Exception {
    var coordinates = new RequestCoordinates();
    when(jsonWebTokenService.getUsernameFromToken("proxy-jwt")).thenReturn("alice");
    when(jsonWebTokenService.getExpirationDateFromToken("proxy-jwt")).thenReturn(new Date());
    when(proxyConfigurationService.validateUserAccess(any(), eq("alice"))).thenReturn(true);
    when(proxyConfigurationService.getRequestCoordinates(any(), eq("alice")))
        .thenReturn(coordinates);
    when(proxyConfigurationService.getConfiguration(any(), any(Long.class), eq(coordinates)))
        .thenThrow(new BadRequestException("Invalid proxy configuration"));

    mvc.perform(post("/api/config/proxy").contentType(APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("https://sitmun.org/problems/bad-request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").value("Invalid proxy configuration"))
        .andExpect(jsonPath("$.instance").value("/api/config/proxy"));
  }
}
