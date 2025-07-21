package org.sitmun.infrastructure.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Dashboard Info Security Test")
class DashboardInfoSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET: /api/dashboard/info should require admin authentication")
    void dashboardInfoShouldRequireAdminAuthentication() throws Exception {
        mvc.perform(get("/api/dashboard/info"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET: /api/dashboard/info should be accessible with admin role")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void dashboardInfoShouldBeAccessibleWithAdminRole() throws Exception {
        mvc.perform(get("/api/dashboard/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dashboard").exists());
    }

    @Test
    @DisplayName("GET: /api/dashboard/info should contain SITMUN metrics for admin")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void dashboardInfoShouldContainSitmunMetricsForAdmin() throws Exception {
        mvc.perform(get("/api/dashboard/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dashboard").exists())
            .andExpect(jsonPath("$.dashboard.total").exists());
    }
} 