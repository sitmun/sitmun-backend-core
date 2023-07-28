package org.sitmun.authentication.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "test" })
class AuthenticationControllerLdapEnabledTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("A user that fails in LDAP and exists in SITMUN, must pass.")
  void ldapFailureUserDetailsSuccessfulLogin() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id_token").exists());
  }

  @Test
  @DisplayName("A user that exists in LDAP and exists in SITMUN, must pass.")
  void successfulLdapLogin() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("internal");
    login.setPassword("password12");

    mvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id_token").exists());
  }

  @Test
  @DisplayName("A user that exists in LDAP and does not exist in SITMUN, must fail.")
  void successfulLdapLoginNoExistsSitmun() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("nositmunuser");
    login.setPassword("nositmunpassword");

    mvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(login)))
        .andExpect(status().isUnauthorized());
  }

}
