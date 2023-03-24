package org.sitmun.domain.application;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Application Repository Data REST test")
class ApplicationRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  private MockHttpServletResponse response;

  @BeforeEach
  public void setup() {
    response = null;
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void create() throws Exception {
    response = mvc.perform(post(URIConstants.APPLICATIONS_URI)
        .content("{" +
          "\"name\":\"test\"," +
          "\"jspTemplate\":\"test\"," +
          "\"type\":\"I\"," +
          "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("POST: createDate is set by the server ")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void createDateValueIsIgnored() throws Exception {
    response = mvc.perform(post(URIConstants.APPLICATIONS_URI)
        .content("{" +
          "\"name\":\"test\"," +
          "\"jspTemplate\":\"test\"," +
          "\"type\":\"I\"," +
          "\"createdDate\":\"2020-01-01\"," +
          "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate").value(matchesPattern("^(?!2020-01-01.*$).*")))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("PUT: createDate can be updated")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void createDateValueCanBeUpdated() throws Exception {
    response = mvc.perform(post(URIConstants.APPLICATIONS_URI)
        .content("{" +
          "\"name\":\"test\"," +
          "\"jspTemplate\":\"test\"," +
          "\"type\":\"I\"," +
          "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andDo(print())
      .andExpect(status().isCreated())
      .andReturn().getResponse();

    String location = response.getHeader("Location");
    assertThat(location).isNotNull();

    mvc.perform(put(location)
        .content("{" +
          "\"name\":\"test\"," +
          "\"jspTemplate\":\"test\"," +
          "\"type\":\"I\"," +
          "\"createdDate\":\"2020-01-01\"," +
          "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.createdDate").value("2020-01-01T00:00:00.000+00:00"));
  }

  @Test
  @DisplayName("POST: situationMap must point to a CartographyPermission")
  void failApplicationWithInvalidMap() throws Exception {
    mvc.perform(post(URIConstants.APPLICATIONS_URI)
        .content("{" +
          "\"name\":\"test\"," +
          "\"jspTemplate\":\"test\"," +
          "\"type\":\"I\"," +
          "\"createdDate\":\"2020-01-01\"," +
          "\"situationMap\":\"http://localhost/api/cartography-group/6\"" +
          "}")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  @DisplayName("PUT: situationMap must point to a CartographyPermission")
  void failUpdateApplicationWithInvalidMap() throws Exception {
    mvc.perform(put(URIConstants.APPLICATION_URI_SITUATION_MAP, 1)
        .content("http://localhost/api/cartography-group/6")
        .contentType("text/uri-list")
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @AfterEach
  public void cleanup() throws Exception {
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
