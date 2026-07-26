package org.sitmun.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.assertj.core.api.Assertions;
import org.sitmun.authentication.SitmunClientTypes;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.authentication.dto.AuthenticationResponse;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class TestUtils {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "admin";
  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
  }

  public static String asJsonString(Object obj) {
    try {
      return mapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns pre-configured {@link HttpHeaders} for admin-authenticated integration tests. Includes
   * {@code Cookie: admin_access_token=<token>} and {@code X-SITMUN-Client: admin} so the JWT filter
   * selects the admin cookie.
   */
  public static HttpHeaders adminAuthHeaders(RestTemplate restTemplate, Integer port) {
    String token = requestAdminToken(restTemplate, port);
    HttpHeaders headers = new HttpHeaders();
    headers.set(
        HttpHeaders.COOKIE, AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME + "=" + token);
    headers.set(SitmunClientTypes.HEADER_NAME, "admin");
    return headers;
  }

  /**
   * Authenticates as admin via the admin login endpoint and returns the raw JWT token value from
   * the {@code admin_access_token} cookie.
   */
  public static String requestAuthorization(RestTemplate restTemplate, Integer port) {
    return requestAdminToken(restTemplate, port);
  }

  private static String requestAdminToken(RestTemplate restTemplate, Integer port) {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername(ADMIN_USERNAME);
    login.setPassword(ADMIN_PASSWORD);
    ResponseEntity<AuthenticationResponse> loginResponse =
        restTemplate.postForEntity(
            "http://localhost:{port}/api/authenticate/admin",
            login,
            AuthenticationResponse.class,
            port);
    Assertions.assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String cookieHeader = loginResponse.getHeaders().getFirst("Set-Cookie");
    if (cookieHeader != null
        && cookieHeader.contains(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME + "=")) {
      return cookieHeader.split(";")[0].split("=", 2)[1];
    }

    return null;
  }

  public static Integer extractId(String url) {
    String[] paths = URI.create(url).getPath().split("/");
    return Integer.parseInt(paths[paths.length - 1]);
  }
}
