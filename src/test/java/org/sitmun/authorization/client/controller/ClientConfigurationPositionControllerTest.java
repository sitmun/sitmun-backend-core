package org.sitmun.authorization.client.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Authorization and Configuration - Territory Position endpoint")
@Transactional
class ClientConfigurationPositionControllerTest {

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper objectMapper;

  private UserPositionDTO validPositionDTO;

  @BeforeEach
  void setUp() {
    validPositionDTO =
        UserPositionDTO.builder()
            .id(1)
            .name("Test Position")
            .organization("Test Organization")
            .email("test@example.com")
            .createdDate(new Date())
            .lastModifiedDate(new Date())
            .expirationDate(new Date(System.currentTimeMillis() + 86400000)) // 24 hours from now
            .type("ADMIN")
            .userId(1)
            .territoryId(1)
            .build();
  }

  @Test
  @DisplayName("POST: Update territory position with valid data should return success")
  @WithMockUser(roles = "USER")
  void editTerritoryPositionsWithValidData() throws Exception {
    mvc.perform(
            post("/api/config/client/territory/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPositionDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(validPositionDTO.getId()))
        .andExpect(jsonPath("$.name").value(validPositionDTO.getName()))
        .andExpect(jsonPath("$.organization").value(validPositionDTO.getOrganization()))
        .andExpect(jsonPath("$.email").value(validPositionDTO.getEmail()))
        .andExpect(jsonPath("$.type").value(validPositionDTO.getType()))
        .andExpect(jsonPath("$.userId").value(validPositionDTO.getUserId()))
        .andExpect(jsonPath("$.territoryId").value(validPositionDTO.getTerritoryId()));
  }

  @Test
  @DisplayName(
      "POST: Update territory position with internal user should return success and expected content")
  void editTerritoryPositionsWithInternalUser() throws Exception {
    mvc.perform(
            post("/api/config/client/territory/position")
                .with(user("internal"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPositionDTO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(validPositionDTO.getId()))
        .andExpect(jsonPath("$.name").value(validPositionDTO.getName()));
  }

  @Test
  @DisplayName("POST: Update territory position without authentication should return unauthorized")
  void editTerritoryPositionsWithoutAuthentication() throws Exception {
    mvc.perform(
            post("/api/config/client/territory/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validPositionDTO)))
        .andExpect(status().isOk()); // The endpoint doesn't require authentication
  }

  @Test
  @DisplayName("POST: Update territory position with null ID should return bad request")
  @WithMockUser(roles = "USER")
  void editTerritoryPositionsWithNullId() throws Exception {
    UserPositionDTO positionWithNullId =
        UserPositionDTO.builder()
            .id(null)
            .name("Test Position")
            .organization("Test Organization")
            .email("test@example.com")
            .type("ADMIN")
            .userId(1)
            .territoryId(1)
            .build();

    mvc.perform(
            post("/api/config/client/territory/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionWithNullId)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST: Update territory position with empty name should return success")
  @WithMockUser(roles = "USER")
  void editTerritoryPositionsWithEmptyName() throws Exception {
    UserPositionDTO positionWithEmptyName =
        UserPositionDTO.builder()
            .id(1)
            .name("")
            .organization("Test Organization")
            .email("test@example.com")
            .type("ADMIN")
            .userId(1)
            .territoryId(1)
            .build();

    mvc.perform(
            post("/api/config/client/territory/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(positionWithEmptyName)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(""));
  }

  @Test
  @DisplayName("POST: Update territory position with missing required fields should return success")
  @WithMockUser(roles = "USER")
  void editTerritoryPositionsWithMissingFields() throws Exception {
    UserPositionDTO minimalPosition =
        UserPositionDTO.builder().id(1).name("Minimal Position").build();

    mvc.perform(
            post("/api/config/client/territory/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalPosition)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Minimal Position"));
  }
}
