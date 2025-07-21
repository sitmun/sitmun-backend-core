package org.sitmun.infrastructure.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Dashboard Health Security Test")
class DashboardHealthSecurityTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: /api/dashboard/health should be accessible without authentication")
  void dashboardHealthShouldBeAccessibleWithoutAuthentication() throws Exception {
    mvc.perform(get("/api/dashboard/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists());
  }
}
