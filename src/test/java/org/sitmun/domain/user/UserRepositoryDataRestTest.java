package org.sitmun.domain.user;

import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Repository Data REST test")
class UserRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  private MockHttpServletResponse response;

  @BeforeEach
  void setup() {
    response = null;
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  @WithMockUser(roles = "ADMIN")
  void create() throws Exception {
    response =
        mvc.perform(
                post(URIConstants.USER_URI)
                    .content(
                        """
                      {
                      "administrator":false,
                      "blocked":false}
                      """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
            .andExpect(jsonPath("$.passwordSet").value(false))
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("POST: rejects an invalid email")
  @WithMockUser(roles = "ADMIN")
  void invalidEmail() throws Exception {
    mvc.perform(
            post(URIConstants.USER_URI)
                .content(
                    """
                    {
                    "administrator":false,
                    "blocked":false,
                    "email":"false"
                    }"""))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errors[?(@.field=='email')].message")
                .value("must be a well-formed email address"));
  }

  @Test
  @DisplayName("POST: accepts a valid email")
  @WithMockUser(roles = "ADMIN")
  void validEmail() throws Exception {
    response =
        mvc.perform(
                post(URIConstants.USER_URI)
                    .content(
                        """
                        {
                        "administrator":false,
                        "blocked":false,
                        "email":"false@false.com"
                        }"""))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location)).andExpect(status().isNoContent());
      }
    }
  }
}
