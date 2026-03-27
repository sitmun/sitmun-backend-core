package org.sitmun.authorization.proxy.controller;

import static org.sitmun.test.URIConstants.CONFIG_PROXY_URI;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP URI template expansion for WMTS uses test service id 7, which is not linked to cartography
 * permissions. Access validation is disabled so {@link ProxyConfigurationControllerTest} can keep
 * default {@code sitmun.proxy-middleware.validate-user-access=true}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "sitmun.proxy-middleware.validate-user-access=false")
@DisplayName("API Proxy — WMTS URI template (validation off for synthetic service)")
class ProxyConfigurationControllerUriTemplateIntegrationTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("POST: WMTS payload URI expands request parameters via HTTP user parametrization")
  @WithMockUser(roles = "PROXY")
  void readWmtsServiceExpandsUriTemplateFromRequestParameters() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "WMTS",
          "typeId": 7,
          "method": "GET",
          "parameters": {
            "tileSet": "utm"
          }
        }
      """;

    mvc.perform(post(CONFIG_PROXY_URI).contentType(APPLICATION_JSON).content(content))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payload.uri").value("https://example.com/wmts/utm/service"));
  }
}
