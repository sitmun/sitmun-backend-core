package org.sitmun.authorization.controller;

import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;


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
   * @throws Exception
   */
  @Test
  void readConnectionPublicUser() throws Exception {
    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-SITMUN-Proxy-key", secret)
          .content("{\"appId\": 1, \"terId\": 0, \"type\": \"SQL\", \"typeId\": 3279, \"method\": \"GET\", \"parameters\": {}, \"requestBody\": null, \"id_token\": null}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("jdbc:database:@host:schema2"));
  }

  /**
   * Test a proxy configuration service for database connection with user token
   * @throws Exception
   */
  @Test
  void readConnectionOtherUser() throws Exception {
	  String token = getUserToken();
	  mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
	      .contentType(MediaType.APPLICATION_JSON)
	      .header("X-SITMUN-Proxy-key", secret)
	      .content("{\"appId\": 1, \"terId\": 0, \"type\": \"SQL\", \"typeId\": 3279, \"method\": \"GET\", \"parameters\": {}, \"requestBody\": null, \"id_token\": \"" + token + "\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("jdbc:database:@host:schema2"));
  }
  
  /**
   * Test a proxy configuration service for service with public user
   * @throws Exception
   */
  @Test
  void readServicePublicUser() throws Exception {
    mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-SITMUN-Proxy-key", secret)
          .content("{\"appId\": 1, \"terId\": 0, \"type\": \"GEO\", \"typeId\": 85, \"method\": \"GET\", \"parameters\": {}, \"requestBody\": null, \"id_token\": null}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("http://sitmun.diba.cat/arcgis/services/PRIVAT/MUNI_DB_BASE/MapServer/WMSServer"));
  }

  /**
   * Test a proxy configuration service for service with user token
   * @throws Exception
   */
  @Test
  void readServiceOtherUser() throws Exception {
	  String token = getUserToken();
	  mvc.perform(post(URIConstants.CONFIG_PROXY_URI)
	      .contentType(MediaType.APPLICATION_JSON)
	      .header("X-SITMUN-Proxy-key", secret)
	      .content("{\"appId\": 1, \"terId\": 0, \"type\": \"GEO\", \"typeId\": 85, \"method\": \"GET\", \"parameters\": {}, \"requestBody\": null, \"id_token\": \"" + token + "\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.payload.uri").value("http://sitmun.diba.cat/arcgis/services/PRIVAT/MUNI_DB_BASE/MapServer/WMSServer"));
  }
}
