package org.sitmun.authentication.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Authentication Controller basic tests")
class AuthenticationControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("POST: Admin user must login")
  void successfulLogin() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME));
  }

  @Test
  @DisplayName("POST: User with wrong credentials must fail")
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
      roles = {"ADMIN"})
  @DisplayName("POST /proxy: Authenticated user must get short-lived token cookie")
  void proxyAuthenticationSuccess() throws Exception {
    mvc.perform(post("/api/authenticate/proxy").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthenticationController.PROXY_TOKEN_COOKIE_NAME));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  @DisplayName("POST /proxy: Cookie must have secure flag when using HTTPS")
  void proxyAuthenticationCookieSecure() throws Exception {
    mvc.perform(post("/api/authenticate/proxy").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().path(AuthenticationController.PROXY_TOKEN_COOKIE_NAME, "/"))
        .andExpect(cookie().secure(AuthenticationController.PROXY_TOKEN_COOKIE_NAME, true));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  @DisplayName("POST /proxy: Cookie must not have secure flag when using HTTP")
  void proxyAuthenticationCookieNotSecure() throws Exception {
    mvc.perform(post("/api/authenticate/proxy").secure(false))
        .andExpect(status().isOk())
        .andExpect(cookie().secure(AuthenticationController.PROXY_TOKEN_COOKIE_NAME, false));
  }
}
