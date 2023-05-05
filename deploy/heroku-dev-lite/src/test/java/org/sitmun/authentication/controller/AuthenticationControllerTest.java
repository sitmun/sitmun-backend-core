package org.sitmun.authentication.controller;

import org.junit.jupiter.api.Test;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc

class AuthenticationControllerTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void successfulLogin() throws Exception {
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
  void loginFailure() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("other");

    mvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(login)))
      .andExpect(status().isUnauthorized());
  }

  /**
   * Authentication of a user that does not exist or is not valid in ldap
   * but exists and is valid in sitmun.
   * @throws Exception
   */
  @Test
  @Profile("ldap")
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
  
  /**
   * Authentication of a valid user in ldap and exists in sitmun.
   * @throws Exception
   */
  @Test
  @Profile("ldap")
  void successfulLdapLogin() throws Exception {
	  UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
	    login.setUsername("user12");
	    login.setPassword("user12");

	    mvc.perform(post("/api/authenticate")
	        .contentType(MediaType.APPLICATION_JSON)
	        .content(TestUtils.asJsonString(login)))
	      .andExpect(status().isOk())
	      .andExpect(jsonPath("$.id_token").exists());
  }

  /**
   * Failed authentication of a valid user in ldap but does not exist in sitmun.
   * @throws Exception
   */
  @Test
  @Profile("ldap")
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
