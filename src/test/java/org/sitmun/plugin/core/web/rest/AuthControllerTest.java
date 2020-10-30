package org.sitmun.plugin.core.web.rest;

import static org.sitmun.plugin.core.test.TestUtils.asJsonString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.web.rest.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void successfulLogin() throws Exception {
    LoginRequest login = new LoginRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(login)))
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
        .content(asJsonString(login)))
        .andExpect(status().isUnauthorized());
  }
}
