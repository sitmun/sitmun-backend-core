package org.sitmun.authorization.controller;

import static org.sitmun.infrastructure.security.core.SecurityConstants.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Proxy")
class ProxyConfigurationControllerTest {

  @Autowired JsonWebTokenService jsonWebTokenService;
  @Autowired private MockMvc mvc;

  @Value("${sitmun.proxy-middleware.secret}")
  private String secret;

  String getUserToken() {
    return jsonWebTokenService.generateToken("admin", new Date());
  }

  /** Test a proxy configuration service for database connection with public user */
  @Test
  @DisplayName("POST: Get database connection details with public user")
  @WithMockUser(roles = "PROXY")
  void readConnectionPublicUser() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 23,
          "method": "GET"
        }
      """;
    mvc.perform(post(CONFIG_PROXY_URI).contentType(APPLICATION_JSON).content(content))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payload.uri").value("jdbc:database:@host:schema2"));
  }

  /** Test a proxy configuration service for database connection with user token */
  @Test
  @DisplayName("POST: Get database connection details with user with token")
  @WithMockUser(roles = "PROXY")
  void readConnectionOtherUser() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 23,
          "method": "GET",
          "token": "%s"
        }
      """
            .formatted(getUserToken());

    mvc.perform(post(CONFIG_PROXY_URI).contentType(APPLICATION_JSON).content(content))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payload.uri").value("jdbc:database:@host:schema2"));
  }

  /** Test a proxy configuration service for service with public user */
  @Test
  @DisplayName("POST: Get WMTS web service details with user with token")
  @WithMockUser(roles = "PROXY")
  void readServicePublicUser() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "WMTS",
          "typeId": 1,
          "method": "GET"
        }
      """;

    mvc.perform(post(CONFIG_PROXY_URI).contentType(APPLICATION_JSON).content(content))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.payload.uri")
                .value("https://geoserveis.icgc.cat/icc_mapesmultibase/utm/wmts/service"))
        .andExpect(jsonPath("$.payload.parameters.format").value("image/jpeg"))
        .andExpect(jsonPath("$.payload.parameters.matrixSet").value("UTM25831"));
  }

  /** Test a proxy configuration service for database connection without sitmun proxy key */
  @Test
  @DisplayName("POST: Unauthorized request returns 401")
  @WithMockUser(roles = {"USER", "ADMIN", "PUBLIC"})
  void proxyUnauthorized() throws Exception {

    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 23,
          "method": "GET"
        }
      """;

    mvc.perform(post(CONFIG_PROXY_URI).contentType(APPLICATION_JSON).content(content))
        .andExpect(status().isForbidden());
  }

  /** Test a proxy configuration service for database connection with pagination */
  @Test
  @DisplayName("POST: Get database query with pagination")
  @WithMockUser(roles = "PROXY")
  void readConnectionWithPagination() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 0,
          "type": "SQL",
          "typeId": 23,
          "method": "GET",
          "parameters": {
            "OFFSET": "100",
            "LIMIT": "100"
          }
        }
      """;

    mvc.perform(post(CONFIG_PROXY_URI).contentType(APPLICATION_JSON).content(content))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payload.sql").value("SELECT * FROM EXAMPLE LIMIT 100 OFFSET 100"));
  }

  @Test
  @DisplayName(
      "POST: Get database query with context filters (USER_ID, TERR_ID, APP_ID and TERR_COD)")
  @WithMockUser(roles = "PROXY")
  void readConnectionWithFixedFilters() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 28,
          "method": "GET",
          "id_token": "%s"
        }
      """
            .formatted(getUserToken());

    mvc.perform(post(CONFIG_PROXY_URI).contentType(APPLICATION_JSON).content(content))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.payload.sql")
                .value(
                    "SELECT * FROM EXAMPLE WHERE USER_ID=1 AND TERR_ID=1 AND APP_ID=1 AND TERR_COD=60001"));
  }

  @Test
  @DisplayName("POST: Get database query with client filters")
  @WithMockUser(roles = "PROXY")
  void readConnectionWithVaryFilters() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 23,
          "method": "GET",
          "parameters": {
            "columnA": "123",
            "columnB": "valueB"
          }
        }
      """;

    mvc.perform(
            post(CONFIG_PROXY_URI)
                .contentType(APPLICATION_JSON)
                .header(PROXY_MIDDLEWARE_KEY, secret)
                .content(content))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.payload.sql")
                .value("SELECT * FROM EXAMPLE WHERE columnA=123 AND columnB='valueB'"));
  }

  @Test
  @DisplayName("POST: Get database query with context and client filters")
  @WithMockUser(roles = "PROXY")
  void readConnectionWithFixedAndVaryFilters() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 27,
          "method": "GET",
          "parameters": {
            "columnA": "123"
          },
          "token": "%s"
        }
      """
            .formatted(getUserToken());

    mvc.perform(
            post(CONFIG_PROXY_URI)
                .contentType(APPLICATION_JSON)
                .header(PROXY_MIDDLEWARE_KEY, secret)
                .content(content))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.payload.sql")
                .value("SELECT * FROM EXAMPLE WHERE TERR_COD=60001 AND columnA=123"));
  }

  @Test
  @DisplayName("POST: Proxy middleware key identifies proxy")
  void proxyMiddlewareKeyIdentifiesProxy() throws Exception {
    String content =
        """
        {
          "appId": 1,
          "terId": 1,
          "type": "SQL",
          "typeId": 27,
          "method": "GET",
          "parameters": {
            "columnA": "123"
          },
          "token": "%s"
        }
      """
            .formatted(getUserToken());

    mvc.perform(
            post(CONFIG_PROXY_URI)
                .contentType(APPLICATION_JSON)
                .header(PROXY_MIDDLEWARE_KEY, secret)
                .content(content))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.payload.sql")
                .value("SELECT * FROM EXAMPLE WHERE TERR_COD=60001 AND columnA=123"));
  }
}
