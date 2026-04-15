package org.sitmun.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
  @DisplayName("POST /proxy: Authenticated user must get short-lived token in response")
  void proxyAuthenticationSuccess() throws Exception {
    MvcResult result = mvc.perform(post("/api/authenticate/proxy").secure(true))
        .andExpect(status().isOk())
        .andReturn();

    String content = result.getResponse().getContentAsString();
    AuthenticationResponse response = objectMapper.readValue(content, AuthenticationResponse.class);
    assertThat(response.getProxyToken()).isNotNull().isNotEmpty();
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  @DisplayName("POST /logout: Authenticated user must logout successfully")
  void logoutSuccess() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }
}
