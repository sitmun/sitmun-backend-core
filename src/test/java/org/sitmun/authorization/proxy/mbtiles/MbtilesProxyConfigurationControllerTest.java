package org.sitmun.authorization.proxy.mbtiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.controllers.ProxyConfigurationController;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator.Failure;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator.Outcome;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator.Result;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProxyConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("POST /api/config/proxy/mbtiles")
class MbtilesProxyConfigurationControllerTest {

  private static final String BODY =
      """
      {
        "appId": 10,
        "territoryId": 20,
        "action": "estimate",
        "bbox": {"minX":0,"minY":0,"maxX":1,"maxY":1},
        "minZoom": 0,
        "maxZoom": 4,
        "srs": "EPSG:25831",
        "services": [{"serviceId":5,"layerIds":[7]}]
      }
      """;

  @Autowired private MockMvc mvc;

  @MockitoBean private ProxyConfigurationService proxyConfigurationService;
  @MockitoBean private ProxyDelegatedTokenAuthenticator delegatedTokenAuthenticator;
  @MockitoBean private MbtilesResourceAccessValidator mbtilesResourceAccessValidator;
  @MockitoBean private TranslationRepository translationRepository;
  @MockitoBean private RequestLocaleResolutionService requestLocaleResolutionService;
  @MockitoBean private CookieService cookieService;

  @MockitoBean private UserApplicationAccessPolicy userApplicationAccessPolicy;

  @BeforeEach
  void stubBearer() {
    when(delegatedTokenAuthenticator.extractBearer("Bearer mobile-proxy"))
        .thenReturn(Optional.of("mobile-proxy"));
  }

  @Test
  @DisplayName("Returns canonical tile request for authorized mobile proxy token")
  void returnsCanonicalConfiguration() throws Exception {
    when(delegatedTokenAuthenticator.authenticate(eq("mobile-proxy"), anyCollection(), eq(true)))
        .thenReturn(new Outcome.Success(new Result("alice", 1_700_000_000_000L)));
    when(mbtilesResourceAccessValidator.authorize(any(), eq("alice"), eq(1_700_000_000_000L)))
        .thenReturn(
            new MbtilesResourceAccessValidator.Outcome.Allowed(
                new MbtilesProxyConfigResponseDto(
                    10,
                    20,
                    "estimate",
                    "alice",
                    1_700_000_000L,
                    new CanonicalTileRequestDto(
                        List.of(
                            new CanonicalTileRequestDto.CanonicalMapServiceDto(
                                "https://tiles.example.com/wms", List.of("roads"), "WMS")),
                        new CanonicalTileRequestDto.CanonicalBboxDto(0, 0, 1, 1, "EPSG:25831"),
                        0,
                        4))));

    mvc.perform(
            post("/api/config/proxy/mbtiles")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer mobile-proxy")
                .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.appId").value(10))
        .andExpect(jsonPath("$.action").value("estimate"))
        .andExpect(
            jsonPath("$.tileRequest.mapServices[0].url").value("https://tiles.example.com/wms"))
        .andExpect(jsonPath("$.tileRequest.mapServices[0].layers[0]").value("roads"));
  }

  @Test
  @DisplayName("Rejects access_token / wrong token kind")
  void rejectsWrongTokenKind() throws Exception {
    when(delegatedTokenAuthenticator.authenticate(eq("mobile-proxy"), anyCollection(), eq(true)))
        .thenReturn(new Outcome.Denied(Failure.WRONG_KIND));

    mvc.perform(
            post("/api/config/proxy/mbtiles")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer mobile-proxy")
                .content(BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
  }

  @Test
  @DisplayName("Returns 403 when resource access is denied")
  void returnsForbiddenWhenResourceDenied() throws Exception {
    when(delegatedTokenAuthenticator.authenticate(
            eq("mobile-proxy"), anyCollection(), anyBoolean()))
        .thenReturn(new Outcome.Success(new Result("alice", 1L)));
    when(mbtilesResourceAccessValidator.authorize(any(), eq("alice"), eq(1L)))
        .thenReturn(new MbtilesResourceAccessValidator.Outcome.Denied("not edition"));

    mvc.perform(
            post("/api/config/proxy/mbtiles")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer mobile-proxy")
                .content(BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.detail").value("not edition"));
  }
}
