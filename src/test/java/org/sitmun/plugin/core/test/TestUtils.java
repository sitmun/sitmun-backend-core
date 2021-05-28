package org.sitmun.plugin.core.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sitmun.plugin.core.web.rest.AuthenticationController;
import org.sitmun.plugin.core.web.rest.dto.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_PASSWORD;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;

public class TestUtils {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "admin";

  public static String asJsonString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void withMockSitmunAdmin(Runnable func) {
    UsernamePasswordAuthenticationToken authReq =
      new UsernamePasswordAuthenticationToken(SITMUN_ADMIN_USERNAME,
        SITMUN_ADMIN_PASSWORD,
        Collections.emptyList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authReq);
    TestSecurityContextHolder.setContext(context);
    func.run();
    TestSecurityContextHolder.clearContext();
  }

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
}
