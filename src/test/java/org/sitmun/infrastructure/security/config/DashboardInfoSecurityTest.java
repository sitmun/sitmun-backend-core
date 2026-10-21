package org.sitmun.infrastructure.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Dashboard Info Security Test")
class DashboardInfoSecurityTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("Anonymous GET /api/dashboard/info returns 401")
  void anonymousGetReturns401() throws Exception {
    mvc.perform(get("/api/dashboard/info")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/dashboard/info returns dashboard.total for an admin")
  @WithMockUser(authorities = "ROLE_ADMIN")
  void dashboardInfoReturnsTotalForAdmin() throws Exception {
    mvc.perform(get("/api/dashboard/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dashboard").exists())
        .andExpect(jsonPath("$.dashboard.total").exists());
  }

  @Test
  @DisplayName("GET /api/dashboard/info returns 403 for a standard user")
  @WithMockUser(authorities = "ROLE_USER")
  void dashboardInfoReturns403ForStandardUser() throws Exception {
    mvc.perform(get("/api/dashboard/info")).andExpect(status().isForbidden());
  }
}
