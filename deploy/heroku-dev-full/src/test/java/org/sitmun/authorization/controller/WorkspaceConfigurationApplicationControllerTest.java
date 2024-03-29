package org.sitmun.authorization.controller;

import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Deprecated
class WorkspaceConfigurationApplicationControllerTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void readPublicUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_APPLICATION_URI, 1, 41))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territory.name").value("Argentona"))
      .andExpect(jsonPath("$.application.title").value("SITMUN - Consulta municipal"))
      .andExpect(jsonPath("$.roles[*].id", containsInAnyOrder(10)));
  }

  @Test
  void readOtherUser() throws Exception {
    mvc.perform(get(URIConstants.WORKSPACE_APPLICATION_URI, 1, 41)
        .with(user("user12"))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.territory.name").value("Argentona"))
      .andExpect(jsonPath("$.application.title").value("SITMUN - Consulta municipal"))
      .andExpect(jsonPath("$.roles[*].id", containsInAnyOrder(14, 17)));
  }

}
