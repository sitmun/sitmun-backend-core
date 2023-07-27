package org.sitmun.authorization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.dto.ConfigProxyRequest;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.HashMap;

import static org.sitmun.infrastructure.security.filter.ProxyTokenFilter.X_SITMUN_PROXY_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TODO Preconfigured parameters are added to the query if they are not present
 *      Example: for the service with id 43, the following parameters can be found at STM_PAR_TREE:
 *     | PSE_TYPE | PSE_NAME    | PSE_VALUE |
 *     |----------|-------------|-----------|
 *     | WMS      | format      | image/png |
 *     | WMS      | version     | 1.3.0     |
 *     | WMS      | service     | WMS       |
 *     | WMS      | transparent | TRUE      |
 *     | WMS      | radius      | 5         |
 *     The PSE_TYPE must be the same as the requested service type (e.g. WMS)
 * TODO Preconfigured parameters are updated in the  query if they are present
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Proxy")
class ProxyConfigurationControllerTest {

  @Autowired
  JsonWebTokenService jsonWebTokenService;
  @Autowired
  private MockMvc mvc;
  @Value("${security.authentication.middleware.secret}")
  private String secret;

  String getUserToken() {
    return jsonWebTokenService.generateToken("admin", new Date());
  }

  /**
   * Test a proxy configuration service for database connection with public user
   */
  @Test
  @DisplayName("Get database connection details with public user")
  void readConnectionPublicUser() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(1).type("SQL").typeId(23).method("GET").build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("jdbc:database:@host:schema2"));
  }

  /**
   * Test a proxy configuration service for database connection with user token
   */
  @Test
  @DisplayName("Get database connection details with user with token")
  void readConnectionOtherUser() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(1).type("SQL").typeId(23).method("GET").token(getUserToken()).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("jdbc:database:@host:schema2"));
  }

  /**
   * Test a proxy configuration service for service with public user
   */
  @Test
  @DisplayName("Get WMTS web service details with user with token")
  void readServicePublicUser() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(1).type("WMTS").typeId(1)
      .method("GET").parameters(new HashMap<>()).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("https://geoserveis.icgc.cat/icc_mapesmultibase/utm/wmts/service"))
      .andExpect(jsonPath("$.payload.parameters.format").value("image/jpeg"))
      .andExpect(jsonPath("$.payload.parameters.matrixSet").value("UTM25831"));
  }

  /**
   * Test a proxy configuration service for database connection without sitmun
   * proxy key
   */
  @Test
  @DisplayName("Unauthorized request returns 401")
  void proxyUnauthorized() throws Exception {

    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(1).type("SQL").typeId(23).method("GET").build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isUnauthorized());
  }

  /**
   * Test a proxy configuration service for database connection with pagination
   */
  @Test
  @DisplayName("Get database query with pagination")
  void readConnectionWithPagination() throws Exception {
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("OFFSET", "100");
    parameters.put("LIMIT", "100");
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(0).type("SQL").typeId(23)
      .method("GET").parameters(parameters).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.sql").value("SELECT * FROM EXAMPLE LIMIT 100 OFFSET 100"));
  }

  @Test
  @DisplayName("Get database query with context filters (USER_ID, TERR_ID, APP_ID and TERR_COD)")
  void readConnectionWithFixedFilters() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(1).type("SQL").typeId(28)
      .method("GET").token(getUserToken()).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.sql")
        .value("SELECT * FROM EXAMPLE WHERE USER_ID=1 AND TERR_ID=1 AND APP_ID=1 AND TERR_COD=60001"));
  }

  @Test
  @DisplayName("Get database query with client filters")
  void readConnectionWithVaryFilters() throws Exception {
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("columnA", "123");
    parameters.put("columnB", "valueB");
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(1).type("SQL").typeId(23)
      .method("GET").parameters(parameters).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.sql").value("SELECT * FROM EXAMPLE WHERE columnA=123 AND columnB='valueB'"));
  }

  @Test
  @DisplayName("Get database query with context and client filters")
  void readConnectionWithFixedAndVaryFilters() throws Exception {
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("columnA", "123");
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(1).type("SQL").typeId(27)
      .method("GET").parameters(parameters).token(getUserToken()).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.sql").value("SELECT * FROM EXAMPLE WHERE TERR_COD=60001 AND columnA=123"));
  }
}
