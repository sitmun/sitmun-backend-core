package org.sitmun.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.sitmun.security.Role;
import org.sitmun.security.web.AuthenticationController;
import org.sitmun.security.web.LoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.security.SecurityConstants.TOKEN_PREFIX;

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
    List<Role> roles = Lists.newArrayList(Role.ROLE_ADMIN, Role.ROLE_USER);
    List<GrantedAuthority> authorities = roles.stream()
      .map(role -> new SimpleGrantedAuthority(role.name()))
      .collect(Collectors.toList());
    UsernamePasswordAuthenticationToken authReq =
      new UsernamePasswordAuthenticationToken("admin", "admin", authorities);
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
