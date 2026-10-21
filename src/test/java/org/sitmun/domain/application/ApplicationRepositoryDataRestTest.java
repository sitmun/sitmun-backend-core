package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Nullable;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Application Repository Data REST test")
class ApplicationRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Nullable private MockHttpServletResponse response;

  @MockitoBean private UserConfigurationRepository userConfigurationRepository;

  @Autowired private ApplicationRepository applicationRepository;

  @Test
  @DisplayName("POST: minimum set of properties")
  @WithMockUser(roles = "ADMIN")
  void create() throws Exception {
    String content =
        """
        {
          "name": "test",
          "jspTemplate": "test",
          "type": "I"
        }""";
    response =
        mvc.perform(post(APPLICATIONS_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("PUT: createDate can be updated")
  @WithMockUser(roles = "ADMIN")
  void createDateValueCanBeUpdated() throws Exception {
    String postContent =
        """
        {
          "name": "test",
          "jspTemplate": "test",
          "type": "I"
        }""";
    String putContent =
        """
        {
          "name": "test",
          "jspTemplate": "test",
          "type": "I",
          "createdDate": "2020-01-01"
        }""";

    response =
        mvc.perform(post(APPLICATIONS_URI).content(postContent))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();

    String location = response.getHeader("Location");
    assertThat(location).isNotNull();

    mvc.perform(put(location).content(putContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdDate").value("2020-01-01T00:00:00.000Z"));
  }

  @Test
  @DisplayName("Private application with a public user exposes the warning")
  @WithMockUser(roles = "ADMIN")
  void returnsWarningWhenPrivateAppHasPublicUserRoles() throws Exception {
    // Given
    Application app = Application.builder().name("Private App").type("I").appPrivate(true).build();

    Integer appId = applicationRepository.save(app).getId();

    // When
    when(userConfigurationRepository.existsByUserUsernameAndRoleIn(
            eq(SecurityConstants.PUBLIC_PRINCIPAL), any(Set.class)))
        .thenReturn(true);

    // Then
    mvc.perform(get(APPLICATION_URI + "?projection=view", appId))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.warnings[0]")
                .value("entity.application.warning.private-application-with-public-user"));

    // Cleanup
    applicationRepository.deleteById(appId);
  }

  @Test
  @DisplayName("POST: responsibleInstitutionName is persisted and trimmed")
  @WithMockUser(roles = "ADMIN")
  void responsibleInstitutionNameIsPersistedAndTrimmed() throws Exception {
    String content =
        """
        {
          "name": "test-institution",
          "jspTemplate": "test",
          "type": "I",
          "responsibleInstitutionName": "  ACME Corp  "
        }""";
    response =
        mvc.perform(post(APPLICATIONS_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.responsibleInstitutionName").value("ACME Corp"))
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("POST: blank responsibleInstitutionName is stored as null")
  @WithMockUser(roles = "ADMIN")
  void blankResponsibleInstitutionNameStoredAsNull() throws Exception {
    String content =
        """
        {
          "name": "test-blank-institution",
          "jspTemplate": "test",
          "type": "I",
          "responsibleInstitutionName": "   "
        }""";
    response =
        mvc.perform(post(APPLICATIONS_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.responsibleInstitutionName").isEmpty())
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
      response = null;
    }
  }
}
