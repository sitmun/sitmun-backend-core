package org.sitmun.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.SitmunClientTypes;
import org.sitmun.authentication.dto.AuthenticationResponse;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Authentication Controller basic tests")
class AuthenticationControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("POST /authenticate: viewer login issues viewer_access_token cookie")
  void viewerLoginIssuesViewerCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME))
        .andExpect(cookie().doesNotExist(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME));
  }

  @Test
  @DisplayName("POST /authenticate: viewer login expires legacy access_token cookie")
  void viewerLoginExpiresLegacyCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /authenticate/admin: admin login issues admin_access_token cookie")
  void adminLoginIssuesAdminCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME))
        .andExpect(cookie().doesNotExist(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME));
  }

  @Test
  @DisplayName("POST /authenticate/admin: admin login expires legacy access_token cookie")
  void adminLoginExpiresLegacyCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /authenticate: wrong credentials returns 401")
  void loginFailure() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("other");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /proxy: Authenticated user must get short-lived token in response")
  void proxyAuthenticationSuccess() throws Exception {
    MvcResult result =
        mvc.perform(post("/api/authenticate/proxy").secure(true))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    AuthenticationResponse response = objectMapper.readValue(content, AuthenticationResponse.class);
    assertThat(response.getProxyToken()).isNotNull().isNotEmpty();
  }

  @Test
  @WithMockUser(
      username = "normal-user",
      roles = {"USER"})
  @DisplayName("POST /proxy: Standard user must get short-lived token in response")
  void proxyAuthenticationSuccessForStandardUser() throws Exception {
    MvcResult result =
        mvc.perform(post("/api/authenticate/proxy").secure(true))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    AuthenticationResponse response = objectMapper.readValue(content, AuthenticationResponse.class);
    assertThat(response.getProxyToken()).isNotNull().isNotEmpty();
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /logout: without selector header clears viewer_access_token")
  void logoutClearsViewerCookie() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /logout: with X-SITMUN-Client: admin clears admin_access_token")
  void logoutWithAdminHeaderClearsAdminCookie() throws Exception {
    mvc.perform(
            post("/api/authenticate/logout")
                .secure(true)
                .header(SitmunClientTypes.HEADER_NAME, "admin"))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /logout: always expires legacy access_token cookie")
  void logoutExpiresLegacyCookie() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @WithMockUser(
      username = "normal-user",
      roles = {"USER"})
  @DisplayName("POST /logout: Standard user must logout successfully")
  void logoutSuccessForStandardUser() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /proxy: Anonymous user must be denied")
  void proxyAuthenticationDeniedForAnonymous() throws Exception {
    mvc.perform(post("/api/authenticate/proxy").secure(true)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /logout: Anonymous user can clear stale session cookie")
  void logoutAllowedForAnonymous() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, 0));
  }
}
