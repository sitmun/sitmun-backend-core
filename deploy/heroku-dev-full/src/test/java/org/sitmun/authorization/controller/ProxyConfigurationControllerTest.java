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
      .appId(1).terId(0).type("SQL").typeId(3279).method("GET").build();

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
      .appId(1).terId(0).type("SQL").typeId(3279).method("GET").token(getUserToken()).build();

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
      .appId(1).terId(0).type("GEO").typeId(85).method("GET").build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("http://sitmun.diba.cat/arcgis/services/PRIVAT/MUNI_DB_BASE/MapServer/WMSServer"));
  }

  /**
   * Test a proxy configuration service for service with user token
   */
  @Test
  void readServiceOtherUser() throws Exception {
    ConfigProxyRequest configProxyRequestBuilder = ConfigProxyRequest.builder()
      .appId(1).terId(0).type("GEO").typeId(85).method("GET").token(getUserToken()).build();

    String json = new ObjectMapper().writeValueAsString(configProxyRequestBuilder);

    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .header(X_SITMUN_PROXY_KEY, secret)
        .content(json))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("http://sitmun.diba.cat/arcgis/services/PRIVAT/MUNI_DB_BASE/MapServer/WMSServer"));
  }
}
