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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.sitmun.plugin.core.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Application Repository Data REST test")
public class ApplicationRepositoryDataRestTest {

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
    response = mvc.perform(post(URIConstants.APPLICATIONS_URI)
      .content("{" +
        "\"name\":\"test\"," +
        "\"jspTemplate\":\"test\"," +
        "\"type\":\"I\"," +
        "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
        "}")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("POST: createDate is set by the server ")
  public void createDateValueIsIgnored() throws Exception {
    response = mvc.perform(post(URIConstants.APPLICATIONS_URI)
      .content("{" +
        "\"name\":\"test\"," +
        "\"jspTemplate\":\"test\"," +
        "\"type\":\"I\"," +
        "\"createdDate\":\"2020-01-01\"," +
        "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
        "}")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.createdDate").value(matchesPattern("^(?!2020-01-01.*$).*")))
      .andReturn().getResponse();
  }

  @Test
  @DisplayName("PUT: createDate can be updated")
  public void createDateValueCanBeUpdated() throws Exception {
    response = mvc.perform(post(URIConstants.APPLICATIONS_URI)
      .content("{" +
        "\"name\":\"test\"," +
        "\"jspTemplate\":\"test\"," +
        "\"type\":\"I\"," +
        "\"situationMap\":\"http://localhost/api/cartography-group/132\"" +
        "}")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
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
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.createdDate").value("2020-01-01T00:00:00.000+00:00"));
  }

  @Test
  @DisplayName("POST: situationMap must point to a CartographyPermission")
  public void failApplicationWithInvalidMap() throws Exception {
    mvc.perform(post(URIConstants.APPLICATIONS_URI)
      .content("{" +
        "\"name\":\"test\"," +
        "\"jspTemplate\":\"test\"," +
        "\"type\":\"I\"," +
        "\"createdDate\":\"2020-01-01\"," +
        "\"situationMap\":\"http://localhost/api/cartography-group/6\"" +
        "}")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].invalidValue").value("C"));
  }

  @Test
  @DisplayName("PUT: situationMap must point to a CartographyPermission")
  public void failUpdateApplicationWithInvalidMap() throws Exception {
    mvc.perform(put(URIConstants.APPLICATION_URI_SITUATION_MAP, 1)
      .content("http://localhost/api/cartography-group/6")
      .contentType("text/uri-list")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
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
          .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
        ).andExpect(status().isNoContent());
      }
    }
  }
}
