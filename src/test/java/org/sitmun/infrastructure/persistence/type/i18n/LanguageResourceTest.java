package org.sitmun.infrastructure.persistence.type.i18n;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Language Resource Data REST test")
class LanguageResourceTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET /api/languages without lang returns endonyms in Guia order")
  void collectionReturnsEndonymsSortedByOrder() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$._embedded.languages[*].shortname",
                contains("ca", "es", "en", "oc-aranes", "fr")))
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'ca')].name", hasItem("Català")))
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'es')].name", hasItem("Castellano")))
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'en')].name", hasItem("English")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'oc-aranes')].name",
                hasItem("Occitan aranés")))
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'fr')].name", hasItem("Français")));
  }

  @Test
  @DisplayName("GET /api/languages?lang=ca keeps endonyms on name; locale labels on translatedName")
  void translatedNamesInCatalan() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI + "?lang=ca"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'es')].name", hasItem("Castellano")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'es')].translatedName",
                hasItem("Castellà")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'oc-aranes')].name",
                hasItem("Occitan aranés")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'oc-aranes')].translatedName",
                hasItem("Aranès")));
  }

  @Test
  @DisplayName("GET /api/languages?lang=es keeps endonyms on name; locale labels on translatedName")
  void translatedNamesInSpanish() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI + "?lang=es"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'es')].name", hasItem("Castellano")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'es')].translatedName",
                hasItem("Castellano")))
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'en')].name", hasItem("English")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'en')].translatedName", hasItem("Inglés")));
  }

  @Test
  @DisplayName(
      "GET /api/languages?lang=oc-aranes keeps endonyms on name; locale labels on translatedName")
  void translatedNamesInOccitan() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI + "?lang=oc-aranes"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.languages[?(@.shortname == 'es')].name", hasItem("Castellano")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'es')].translatedName",
                hasItem("Castelhan")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'oc-aranes')].name",
                hasItem("Occitan aranés")))
        .andExpect(
            jsonPath(
                "$._embedded.languages[?(@.shortname == 'oc-aranes')].translatedName",
                hasItem("Occitan aranés")));
  }

  @Test
  @Transactional
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH order persists on language")
  void patchOrderPersists() throws Exception {
    mvc.perform(
            patch(URIConstants.LANGUAGES_URI + "/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"order\": 99}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order").value(99));

    mvc.perform(get(URIConstants.LANGUAGES_URI + "/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order").value(99));
  }

  @Test
  @DisplayName("GET /api/languages returns enabled true by default")
  void enabledTrueByDefault() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.languages[*].enabled", contains(true, true, true, true, true)));
  }

  @Test
  @DisplayName("GET /api/languages?projection=view includes enabled for admin list")
  void viewProjectionIncludesEnabled() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI + "?projection=view"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.languages[*].enabled", contains(true, true, true, true, true)))
        .andExpect(jsonPath("$._embedded.languages[*].order", contains(1, 2, 3, 4, 5)));
  }

  @Test
  @Transactional
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH disable non-default language succeeds")
  void patchDisableNonDefaultLanguageSucceeds() throws Exception {
    mvc.perform(
            patch(URIConstants.LANGUAGES_URI + "/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  @Transactional
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH disable current default language fails")
  void patchDisableDefaultLanguageFails() throws Exception {
    mvc.perform(
            patch(URIConstants.LANGUAGES_URI + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": false}"))
        .andExpect(status().isBadRequest());
  }
}
