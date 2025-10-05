package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
  @DisplayName("POST: createDate is set by the server ")
  @WithMockUser(roles = "ADMIN")
  void createDateValueIsIgnored() throws Exception {
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
            .andExpect(jsonPath("$.createdDate").value(matchesPattern("^(?!2020-01-01.*$).*")))
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
        .andExpect(jsonPath("$.createdDate").value("2020-01-01T00:00:00.000+00:00"));
  }

  @Test
  @DisplayName("GET: Should return warning when private application has PUBLIC user")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnWarningWhenPrivateAppHasPublicUserRoles() throws Exception {
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
  @DisplayName("GET: Should return no warnings when private application has no PUBLIC user")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnNoWarningsWhenPrivateAppHasNoPublicUserRoles() throws Exception {
    // Given
    Application app = Application.builder().name("Private App").type("I").appPrivate(true).build();

    Integer appId = applicationRepository.save(app).getId();

    // When
    when(userConfigurationRepository.existsByUserUsernameAndRoleIn(
            eq(SecurityConstants.PUBLIC_PRINCIPAL), any(Set.class)))
        .thenReturn(false);

    // Then
    mvc.perform(get(APPLICATION_URI + "?projection=view", appId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.warnings").isEmpty());

    // Cleanup
    applicationRepository.deleteById(appId);
  }

  @Test
  @DisplayName("Should return no warnings in public applications")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnNoWarningsWhenPublicAppHasPublicUserRoles() throws Exception {
    // Given
    Application app = Application.builder().name("Private App").type("I").appPrivate(false).build();

    Integer appId = applicationRepository.save(app).getId();

    // When

    // Then
    mvc.perform(get(APPLICATION_URI + "?projection=view", appId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.warnings").isEmpty());

    // Cleanup
    applicationRepository.deleteById(appId);
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
