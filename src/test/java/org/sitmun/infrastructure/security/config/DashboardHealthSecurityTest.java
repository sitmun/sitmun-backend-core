package org.sitmun.infrastructure.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.startup.BuiltInUserStartupStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Dashboard Health Security Test")
class DashboardHealthSecurityTest {

  @Autowired private MockMvc mvc;
  @Autowired private BuiltInUserStartupStatus builtInUserStartupStatus;

  @AfterEach
  void restoreReadyStatus() {
    builtInUserStartupStatus.markReady();
  }

  @Test
  @DisplayName("GET: /api/dashboard/health should be accessible without authentication")
  void dashboardHealthShouldBeAccessibleWithoutAuthentication() throws Exception {
    builtInUserStartupStatus.markReady();
    mvc.perform(get("/api/dashboard/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").exists());
  }

  @Test
  @DisplayName("GET: /api/dashboard/startup is public and returns ready without reason")
  void startupReadyIsPublic() throws Exception {
    builtInUserStartupStatus.markReady();
    mvc.perform(get("/api/dashboard/startup"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ready"))
        .andExpect(jsonPath("$.reason").doesNotExist());
  }

  @Test
  @DisplayName("GET: /api/dashboard/startup returns blocked with stable reason only")
  void startupBlockedExposesStableReason() throws Exception {
    builtInUserStartupStatus.markBlocked(
        BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD);

    mvc.perform(get("/api/dashboard/startup"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("blocked"))
        .andExpect(
            jsonPath("$.reason")
                .value(BuiltInUserStartupStatus.REASON_ADMIN_MISSING_BOOTSTRAP_PASSWORD))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.secret").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist());

    mvc.perform(get("/api/dashboard/health"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  @Test
  @DisplayName("GET: /api/dashboard/startup returns initializing without reason")
  void startupInitializingIsPublic() throws Exception {
    builtInUserStartupStatus.markInitializing();
    mvc.perform(get("/api/dashboard/startup"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("initializing"))
        .andExpect(jsonPath("$.reason").doesNotExist());
  }
}
