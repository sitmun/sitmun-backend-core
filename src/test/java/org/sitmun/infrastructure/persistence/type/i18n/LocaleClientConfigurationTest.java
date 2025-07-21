package org.sitmun.infrastructure.persistence.type.i18n;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@DisplayName("Locale Client Configuration Test")
class LocaleClientConfigurationTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Obtain original client profile")
  void obtainOriginalVersion() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI, 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backgrounds[0].title").value("Background Map"));
  }

  @Test
  @DisplayName("GET: Obtain client profile in Spanish")
  void obtainOriginalClientProfile() throws Exception {
    mvc.perform(get(URIConstants.CONFIG_CLIENT_PROFILE_URI + "?lang=es", 1, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backgrounds[0].title").value("Mapa de fondo"));
  }
}
