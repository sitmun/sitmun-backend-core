package org.sitmun.domain.application.parameter;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Application Parameter Repository Data rest test")
class ApplicationParameterRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve filtered application parameters")
  void filteredApplicationParameters() throws Exception {
    mvc.perform(
            get(URIConstants.APPLICATION_PARAMETERS_URI, 1, "type", "PRINT_TEMPLATE")
                .with(user(Fixtures.admin())))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.application-parameters", hasSize(4)));
  }
}
