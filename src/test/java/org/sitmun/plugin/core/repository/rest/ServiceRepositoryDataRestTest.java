package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.sitmun.plugin.core.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Service Repository Data REST test")
public class ServiceRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  private MockHttpServletResponse response;

  @BeforeEach
  public void setup() {
    response = null;
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  public void create() throws Exception {
    response = mvc.perform(post(URIConstants.SERVICE_URI)
      .content("{" +
        "\"name\":\"test\"," +
        "\"type\":\"WMS\"," +
        "\"blocked\":false," +
        "\"serviceURL\":\"https://www.example.com\"" +
        "}")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
      .andReturn().getResponse();
  }

  @AfterEach
  public void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location)
          .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
        ).andExpect(status().isNoContent());
      }
    }
  }
}
