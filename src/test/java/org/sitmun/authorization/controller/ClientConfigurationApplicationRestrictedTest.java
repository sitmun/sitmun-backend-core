package org.sitmun.authorization.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.sitmun.infrastructure.security.core.SecurityConstants.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Client Configuration Application with restricted access Tests")
class ClientConfigurationApplicationRestrictedTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ApplicationRepository applicationRepository;

  @Test
  @DisplayName("Should filter out private applications for public USER in applications list")
  void shouldFilterOutPrivateApplicationsForPublicUserInApplicationsList() throws Exception {
    // Given
    Application privateApp =
        Application.builder().id(1).title("Private App").appPrivate(true).build();
    Application publicApp =
        Application.builder().id(2).title("Public App").appPrivate(false).build();

    // When
    when(applicationRepository.findByPublicUser(eq(PUBLIC_PRINCIPAL), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(publicApp)));
    when(applicationRepository.findByUser(eq(PUBLIC_PRINCIPAL), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(publicApp, privateApp)));

    // Then
    mockMvc
        .perform(get(CONFIG_CLIENT_APPLICATION_URI).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].title").value("Public App"))
        .andExpect(jsonPath("$.content[0].appPrivate").value(false));
  }

  @Test
  @DisplayName("Should not filter applications for non-public USER in applications list")
  @WithMockUser(username = "user", roles = "USER")
  void shouldNotFilterApplicationsForNonPublicUserInApplicationsList() throws Exception {
    // Given
    Application privateApp =
        Application.builder().id(1).title("Private App").appPrivate(true).build();
    Application publicApp =
        Application.builder().id(2).title("Public App").appPrivate(false).build();

    // When
    when(applicationRepository.findByPublicUser(eq("user"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(publicApp)));
    when(applicationRepository.findByUser(eq("user"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(publicApp, privateApp)));

    // Then
    mockMvc
        .perform(get(CONFIG_CLIENT_APPLICATION_URI).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].title").value("Public App"))
        .andExpect(jsonPath("$.content[0].appPrivate").value(false))
        .andExpect(jsonPath("$.content[1].title").value("Private App"))
        .andExpect(jsonPath("$.content[1].appPrivate").value(true));
  }

  @Test
  @DisplayName("Should return 401 when public user tries to access private application")
  void shouldReturn403WhenPublicUserTriesToAccessPrivateApplication() throws Exception {
    // Given
    Application privateApp =
        Application.builder().id(1).name("Private App").appPrivate(true).build();

    // When
    when(applicationRepository.findById(1)).thenReturn(Optional.of(privateApp));

    // Then
    mockMvc
        .perform(get(CONFIG_CLIENT_APPLICATION_TERRITORIES_URI, 1).contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should work when a non-public user tries to access private application")
  @WithMockUser(roles = "USER")
  void shouldWorkWhenAdminUserTriesToAccessPrivateApplication() throws Exception {
    // Given
    Application privateApp =
        Application.builder().id(1).name("Private App").appPrivate(true).build();

    // When
    when(applicationRepository.findById(1)).thenReturn(Optional.of(privateApp));

    // Then
    mockMvc
        .perform(get(CONFIG_CLIENT_APPLICATION_TERRITORIES_URI, 1).contentType(APPLICATION_JSON))
        .andExpect(status().isOk());
  }
}
