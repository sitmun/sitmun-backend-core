package org.sitmun.authorization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest
@AutoConfigureMockMvc
class ProxyConfigurationControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  JsonWebTokenService jsonWebTokenService;

  @Value("${security.authentication.middleware.secret}")
  private String secret;

  String getUserToken() {
    return jsonWebTokenService.generateToken("admin", new Date());
  }

  /**
   * Test a proxy configuration service for database connection with public user
   */
  @Test
  void readConnectionPublicUser() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
        .appId(1).terId(0).type("SQL").typeId(30).method("GET").build();

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
  void readConnectionOtherUser() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
        .appId(1).terId(0).type("SQL").typeId(30).method("GET").token(getUserToken()).build();

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
  void readServicePublicUser() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
        .appId(1).terId(0).type("GEO").typeId(1).method("GET").build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payload.uri").value("https://geoserveis.icgc.cat/icc_mapesmultibase/utm/wmts/service"));
  }

  /**
   * Test a proxy configuration service for database connection without sitmun
   * proxy key
   */
  @Test
  void proxyUnauthorized() throws Exception {

    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
        .appId(1).terId(0).type("SQL").typeId(30).method("GET").build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Test a proxy configuration service for database connection with pagination
   * 
   * @throws Exception
   */
  @Test
  void readConnectionWithPagination() throws Exception {
    HashMap<String, String> parameters = new HashMap<>();
    parameters.put("offset", "1");
    parameters.put("limit", "100");
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
        .appId(1).terId(0).type("SQL").typeId(30)
        .method("GET").parameters(parameters).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.payload.uri").value("jdbc:database:@host:schema2"));
  }
}
