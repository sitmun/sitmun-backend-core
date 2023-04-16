package org.sitmun.domain.task.type;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

class TaskTypesRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void definedSpecifications() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(URIConstants.TASK_TYPES_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
      ).andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.task-types[?(@.specification != null)]", Matchers.hasSize(9)));
  }
}
