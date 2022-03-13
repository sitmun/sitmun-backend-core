package org.sitmun.common.security.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.security.web.LoginRequest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class AuthenticationControllerTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void successfulLogin() throws Exception {
    LoginRequest login = new LoginRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(login)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id_token").exists());
  }

  @Test
  public void loginFailure() throws Exception {
    LoginRequest login = new LoginRequest();
    login.setUsername("admin");
    login.setPassword("other");

    mvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(login)))
      .andExpect(status().isUnauthorized());
  }
}
