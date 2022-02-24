package org.sitmun.api;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.security.web.AuthenticationController;
import org.sitmun.security.web.LoginRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.security.SecurityConstants.TOKEN_PREFIX;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@DisplayName("Service Capabilities Extractor integration test")
public class ServiceCapabilitiesExtractorIntegrationTest {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "admin";

  @LocalServerPort
  private int port;

  private RestTemplate restTemplate;

  public static String requestAuthorization(RestTemplate restTemplate, Integer port) {
    LoginRequest login = new LoginRequest();
    login.setUsername(ADMIN_USERNAME);
    login.setPassword(ADMIN_PASSWORD);
    ResponseEntity<AuthenticationController.JWTToken> loginResponse =
      restTemplate
        .postForEntity("http://localhost:{port}/api/authenticate", login, AuthenticationController.JWTToken.class, port);
    assertThat(loginResponse.getBody()).isNotNull();
    return TOKEN_PREFIX + loginResponse.getBody().getIdToken();
  }

  @BeforeEach
  public void setup() {
    restTemplate = new RestTemplate();
  }

  @Test
  @DisplayName("A request with a percent-encoded ampersand succeeds")
  public void usePercentEncodedAmpersand() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_STRING, requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
      restTemplate
        .exchange("http://localhost:" + port + "/api/helpers/capabilities?url=https://sitmun.diba.cat/wms/servlet/ACE1M?request=GetCapabilities%26service=WMS", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext context = JsonPath.parse(response.getBody());

    assertTrue(context.read("$.success", Boolean.class));
    assertEquals("OGC:WMS 1.3.0", context.read("$.type", String.class));
    assertTrue(context.read("$.asText", String.class).startsWith("<?xml"));
    assertNotNull(context.read("$.asJson.WMS_Capabilities", LinkedHashMap.class));
    assertTrue(context.read("$.asJson.WMS_Capabilities", LinkedHashMap.class).size() > 0);
  }

  @Test
  @DisplayName("A request with an ampersand fails")
  public void failWithAmpersand() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HEADER_STRING, requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      restTemplate
        .exchange("http://localhost:" + port + "/api/helpers/capabilities?url=https://sitmun.diba.cat/wms/servlet/ACE1M?request=GetCapabilities&service=WMS", HttpMethod.GET, entity, String.class);
      fail();
    } catch (HttpClientErrorException.BadRequest badRequest) {
      DocumentContext context = JsonPath.parse(badRequest.getResponseBodyAsString());

      assertFalse(context.read("$.success", Boolean.class));
      assertTrue(context.read("$.asText", String.class).startsWith("<?xml"));
    }
  }
}