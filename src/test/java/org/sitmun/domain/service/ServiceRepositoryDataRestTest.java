package org.sitmun.domain.service;

import org.junit.jupiter.api.*;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Service Repository Data REST test")
class ServiceRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  private MockHttpServletResponse response;

  @BeforeEach
  void setup() {
    response = null;
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  void create() throws Exception {
    response = mvc.perform(post(URIConstants.SERVICES_URI)
        .content("{" +
          "\"name\":\"test\"," +
          "\"type\":\"WMS\"," +
          "\"blocked\":false," +
          "\"serviceURL\":\"https://www.example.com\"" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
      .andReturn().getResponse();
  }

  @AfterEach
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location)
          .with(user(Fixtures.admin()))
        ).andExpect(status().isNoContent());
      }
    }
  }
}
