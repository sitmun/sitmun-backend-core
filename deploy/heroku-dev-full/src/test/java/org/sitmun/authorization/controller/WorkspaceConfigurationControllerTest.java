package org.sitmun.authorization.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Deprecated
class WorkspaceConfigurationControllerTest {

  @Autowired
  private JsonWebTokenService jsonWebTokenService;

  @Autowired
  private MockMvc mvc;

  @Test
  void readPublicUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territories[*].userConfigurations[*].role.applications[*]", Matchers.hasSize(7)));
  }

  @Test
  void readOtherUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_URI)
        .with(user("user12"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territories[*].userConfigurations[*].role.applications[*]", Matchers.hasSize(465)));
  }

  @Test
  void readOtherUserWithToken() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_URI)
        .header(HttpHeaders.AUTHORIZATION, "Bearer "+jsonWebTokenService.generateToken("user12", new Date()))
      ).andExpect(status().isOk())
      .andExpect(jsonPath("$.territories[*].userConfigurations[*].role.applications[*]", Matchers.hasSize(465)));
  }
}
