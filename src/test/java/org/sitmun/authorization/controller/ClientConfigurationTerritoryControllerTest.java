package org.sitmun.authorization.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Authorization and Configuration - Territories endpoint")
class ClientConfigurationTerritoryControllerTest {

  private static final String TERRITORY_A_NAME = "Territory A";
  private static final String TERRITORY_B_NAME = "Territory B";

  @Autowired private MockMvc mvc;

  @MockitoBean private TerritoryRepository territoryRepository;

  @Test
  @DisplayName("GET: Retrieve list of territories for PUBLIC (implicit) role")
  void readPublicUser() throws Exception {
    // Given
    Territory territoryA = Territory.builder().id(1).name(TERRITORY_A_NAME).build();

    Territory territoryB = Territory.builder().id(1).name(TERRITORY_B_NAME).build();

    // When
    when(territoryRepository.findByPublicUser(
            eq(SecurityConstants.PUBLIC_PRINCIPAL), any(Pageable.class)))
        .thenReturn(new PageImpl<>(Arrays.asList(territoryA, territoryB)));

    // Then
    mvc.perform(get(URIConstants.CONFIG_CLIENT_TERRITORY_URI))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.page.size", is(2)))
        .andExpect(jsonPath("$.page.number", is(0)))
        .andExpect(jsonPath("$.page.totalPages", is(1)))
        .andExpect(jsonPath("$.content[*].name", hasItems(TERRITORY_A_NAME, TERRITORY_B_NAME)));
  }

  @Test
  @DisplayName("GET: Retrieve list of territories for USER role")
  @WithMockUser(roles = "USER")
  void readOtherUser() throws Exception {
    // Given
    Territory territoryA = Territory.builder().id(1).name(TERRITORY_A_NAME).build();

    Territory territoryB = Territory.builder().id(1).name(TERRITORY_B_NAME).build();

    // When
    when(territoryRepository.findByRestrictedUser(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(Arrays.asList(territoryA, territoryB)));

    // Then
    mvc.perform(get(URIConstants.CONFIG_CLIENT_TERRITORY_URI))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.page.size", is(2)))
        .andExpect(jsonPath("$.page.number", is(0)))
        .andExpect(jsonPath("$.page.totalPages", is(1)))
        .andExpect(jsonPath("$.content[*].name", hasItems(TERRITORY_A_NAME, TERRITORY_B_NAME)));
  }
}
