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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Data REST integration test for {@link TranslationRepository}.
 *
 * <p>Validates that the {@code byElement} search resource properly initializes {@code
 * Translation.language} for {@link TranslationProjection} rendering under default OSIV.
 *
 * <p>The projection accesses {@code #{target.language?.name}} and {@code
 * #{target.language?.shortname}} during HAL/JSON serialization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Translation Repository Data REST test")
class TranslationRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET byElement: Projection renders language fields (default OSIV)")
  @WithMockUser(roles = "ADMIN")
  void byElement_projectionRendersLanguageFields_defaultOSIV() throws Exception {
    mvc.perform(get(URIConstants.TRANSLATIONS_URI + "/search/byElement?element=1&column=Language"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.translations").isArray())
        .andExpect(jsonPath("$._embedded.translations[0].languageName").exists())
        .andExpect(jsonPath("$._embedded.translations[0].languageShortname").exists())
        .andExpect(jsonPath("$._embedded.translations[0].languageName").isNotEmpty())
        .andExpect(jsonPath("$._embedded.translations[0].languageShortname").isNotEmpty());
  }
}
