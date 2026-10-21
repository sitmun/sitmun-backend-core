package org.sitmun.authorization.client.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.client.mapper.ProfileMapper;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.authorization.client.service.ClientUserPositionService;
import org.sitmun.authorization.client.service.MobileEditionAccessService;
import org.sitmun.authorization.client.service.ProxyMiddlewareUrlResolver;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.sitmun.infrastructure.persistence.type.i18n.DatabaseDefaultLanguageResolver;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ClientConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Territory position endpoint controller contract")
class ClientConfigurationPositionControllerTest {

  @Autowired private MockMvc mvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthorizationService authorizationService;

  @MockitoBean private ClientUserPositionService clientUserPositionService;

  @MockitoBean private MobileEditionAccessService mobileEditionAccessService;

  @MockitoBean private ProfileMapper profileMapper;

  @MockitoBean private TranslationRepository translationRepository;

  @MockitoBean private DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver;

  @MockitoBean private RequestLocaleResolutionService requestLocaleResolutionService;

  @MockitoBean private CookieService cookieService;

  @MockitoBean private UserApplicationAccessPolicy userApplicationAccessPolicy;

  @MockitoBean private ProxyMiddlewareUrlResolver proxyMiddlewareUrlResolver;

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
            .expirationDate(new Date(System.currentTimeMillis() + 86400000))
            .type("ADMIN")
            .userId(1)
            .territoryId(1)
            .build();
  }

  @Test
  @DisplayName("POST: owner update returns 200 with persisted body")
  @WithMockUser(username = "alice", roles = "USER")
  void editTerritoryPositionsWithValidData() throws Exception {
    when(clientUserPositionService.updateOwnedPosition(eq("alice"), any(UserPositionDTO.class)))
        .thenReturn(validPositionDTO);

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
}
