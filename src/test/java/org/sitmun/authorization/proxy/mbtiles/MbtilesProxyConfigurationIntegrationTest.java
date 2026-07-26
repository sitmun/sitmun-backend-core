package org.sitmun.authorization.proxy.mbtiles;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/config/proxy/mbtiles integration")
class MbtilesProxyConfigurationIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ApplicationRepository applicationRepository;

  @Value("${sitmun.proxy-middleware.secret}")
  private String proxySecret;

  @BeforeEach
  void ensureEditionApplication() {
    Application app = applicationRepository.findById(1).orElseThrow();
    app.setType("ED");
    applicationRepository.saveAndFlush(app);
  }

  @Test
  @DisplayName("Returns canonical tile request for seeded background layer")
  void returnsCanonicalForSeededLayer() throws Exception {
    String proxyToken = mobileProxyToken();

    String body =
        """
        {
          "appId": 1,
          "territoryId": 1,
          "action": "estimate",
          "bbox": {"minX":0,"minY":0,"maxX":1,"maxY":1},
          "minZoom": 0,
          "maxZoom": 1,
          "srs": "EPSG:3857",
          "services": [{"serviceId":1,"layerIds":[1]}]
        }
        """;

    mvc.perform(
            post("/api/config/proxy/mbtiles")
                .contentType(APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + proxyToken)
                .header(SecurityConstants.PROXY_MIDDLEWARE_KEY, proxySecret)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tileRequest.mapServices[0].url").isNotEmpty());
  }

  private String mobileProxyToken() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");
    MvcResult mobile =
        mvc.perform(
                post("/api/authenticate/mobile")
                    .contentType(APPLICATION_JSON)
                    .content(TestUtils.asJsonString(login)))
            .andExpect(status().isOk())
            .andReturn();
    String accessToken =
        objectMapper
            .readTree(mobile.getResponse().getContentAsString())
            .get("access_token")
            .asText();

    MvcResult proxy =
        mvc.perform(
                post("/api/authenticate/proxy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(proxy.getResponse().getContentAsString());
    return body.get("proxy_token").asText();
  }
}
