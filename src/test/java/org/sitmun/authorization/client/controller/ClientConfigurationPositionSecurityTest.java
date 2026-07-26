package org.sitmun.authorization.client.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.service.ClientUserPositionService;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Territory position endpoint security")
@Transactional
class ClientConfigurationPositionSecurityTest {

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ClientUserPositionService clientUserPositionService;

  @Test
  @DisplayName("POST without USER role is rejected before service invocation")
  void editTerritoryPositionsWithoutAuthentication() throws Exception {
    UserPositionDTO position =
        UserPositionDTO.builder()
            .id(1)
            .name("Test Position")
            .organization("Test Organization")
            .email("test@example.com")
            .createdDate(new Date())
            .type("ADMIN")
            .userId(1)
            .territoryId(1)
            .build();

    mvc.perform(
            post("/api/config/client/territory/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(position)))
        .andExpect(status().isUnauthorized());

    verify(clientUserPositionService, never()).updateOwnedPosition(anyString(), any());
  }
}
