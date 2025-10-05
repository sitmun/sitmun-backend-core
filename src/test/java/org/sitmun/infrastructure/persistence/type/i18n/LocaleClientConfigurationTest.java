package org.sitmun.infrastructure.persistence.type.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.LogTestUtils;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Slf4j
@DisplayName("Locale Client Configuration Test")
class LocaleClientConfigurationTest {

  @Autowired private MockMvc mvc;

  private LogTestUtils sqlLogger;

  @BeforeEach
  void setUp() {
    sqlLogger = new LogTestUtils("org.hibernate.SQL");
  }

  @AfterEach
  void tearDown() {
    if (sqlLogger != null) {
      sqlLogger.stopCapturing();
    }
  }

  @Test
  @DisplayName("GET: Obtain original client profile")
  void obtainOriginalVersion() throws Exception {
    sqlLogger.startCapturing();
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backgrounds[0].title").value("Background Map"));
    detectUnexpectedUpdates();
  }

  @Test
  @DisplayName("GET: Obtain client profile in Spanish")
  void obtainOriginalClientProfile() throws Exception {
    sqlLogger.startCapturing();
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI + "?lang=es", 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backgrounds[0].title").value("Mapa de fondo"));
    detectUnexpectedUpdates();
  }

  void detectUnexpectedUpdates() {
    List<String> messages = sqlLogger.getLogMessagesContaining("^update ");
    messages.forEach(msg -> log.error("SQL Update detected: {}", msg));
    assertTrue(messages.isEmpty(), "Unexpected SQL Update statements detected");
  }
}
