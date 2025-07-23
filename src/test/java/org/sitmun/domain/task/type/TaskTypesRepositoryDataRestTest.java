package org.sitmun.domain.task.type;

import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Task Types Repository Data REST Test")
class TaskTypesRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve task types with defined specifications")
  @WithMockUser(roles = "ADMIN")
  void definedSpecifications() throws Exception {
    mvc.perform(get(TASK_TYPES_URI).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.task-types[?(@.specification != null)]", Matchers.hasSize(9)));
  }
}
