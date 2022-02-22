package org.sitmun.repository.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class TaskTypesRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void definedSpecifications() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(URIConstants.TASK_TYPES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.task-types[?(@.specification != null)]", hasSize(9)));
  }
}
