package org.sitmun.authentication.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.AuthProviderIds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthenticationInfoController integration tests")
class AuthenticationInfoControllerIntegrationTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET enabled-methods returns 200 with database provider")
  void getEnabledMethods_returnsDatabaseProvider() throws Exception {
    mvc.perform(get("/api/auth/enabled-methods"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(AuthProviderIds.DATABASE));
  }
}
