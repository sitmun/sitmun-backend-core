package org.sitmun.authorization.client.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.client.dto.ProfileMapper;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientConfigurationController.class)
@DisplayName("Mbtiles Configuration Integration Test")
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"sitmun.mbtiles.url=https://test.example.com/mbtiles"})
class MbtilesConfigurationTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private AuthorizationService authorizationService;

  @MockitoBean private UserPositionRepository userPositionRepository;

  @MockitoBean private ProfileMapper profileMapper;

  @Test
  @DisplayName("Mbtiles URL should be injected from configuration and used in applications")
  @WithMockUser(username = "testuser", roles = "USER")
  void mbtilesUrlShouldBeInjectedFromConfiguration() throws Exception {
    // Given
    Application app = new Application();
    app.setId(1);
    app.setTitle("Test Application");
    app.setType("T");

    Page<Application> page = new PageImpl<>(Collections.singletonList(app));
    when(authorizationService.findApplicationsByUser(anyString(), any(Pageable.class)))
        .thenReturn(page);

    // When & Then
    mvc.perform(get("/api/config/client/application"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[0].config.mbtilesUrl").value("https://test.example.com/mbtiles"));
  }
}
