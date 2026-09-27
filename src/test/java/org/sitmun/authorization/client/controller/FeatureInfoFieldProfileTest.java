package org.sitmun.authorization.client.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.sitmun.test.URIConstants.CONFIG_CLIENT_PROFILE_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.parameter.CartographyParameter;
import org.sitmun.domain.cartography.parameter.CartographyParameterRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.sitmun.infrastructure.persistence.type.i18n.Translation;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Client profile feature information fields")
class FeatureInfoFieldProfileTest {

  @Autowired private MockMvc mvc;
  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private CartographyParameterRepository cartographyParameterRepository;
  @Autowired private LanguageRepository languageRepository;
  @Autowired private TranslationRepository translationRepository;

  @Test
  @DisplayName("GET: no INFO rows omits the field list")
  void omittedWhenEmpty() throws Exception {
    mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1).param("lang", "es"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.layers[?(@.id=='layer/3')].featureInfoFields").doesNotExist());
  }

  @Test
  @DisplayName("GET: lang returns the translated label and the original property name")
  void translatedLabelKeepsPropertyName() throws Exception {
    var cartography = cartographyRepository.findById(1).orElseThrow();
    var code =
        cartographyParameterRepository.saveAndFlush(
            CartographyParameter.builder()
                .cartography(cartography)
                .name("code")
                .value("Code")
                .type("INFO")
                .format("T")
                .order(0)
                .build());
    var season =
        cartographyParameterRepository.saveAndFlush(
            CartographyParameter.builder()
                .cartography(cartography)
                .name("season")
                .value("Season")
                .type("INFO")
                .format("T")
                .order(2)
                .build());
    var spanish = languageRepository.findByShortname("es").orElseThrow();
    var translation =
        translationRepository.saveAndFlush(
            Translation.builder()
                .element(season.getId())
                .column("CartographyParameter.value")
                .language(spanish)
                .translation("Temporada")
                .build());
    try {
      mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1).param("lang", "es"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath("$.layers[?(@.id=='layer/1')].featureInfoFields[0].name", hasItem("code")))
          .andExpect(
              jsonPath("$.layers[?(@.id=='layer/1')].featureInfoFields[0].label", hasItem("Code")))
          .andExpect(
              jsonPath("$.layers[?(@.id=='layer/1')].featureInfoFields[1].name", hasItem("season")))
          .andExpect(
              jsonPath(
                  "$.layers[?(@.id=='layer/1')].featureInfoFields[1].label", hasItem("Temporada")));

      mvc.perform(get(CONFIG_CLIENT_PROFILE_URI, 1, 1).param("lang", "en"))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath(
                  "$.layers[?(@.id=='layer/1')].featureInfoFields[1].label", hasItem("Season")));
    } finally {
      translationRepository.delete(translation);
      cartographyParameterRepository.delete(season);
      cartographyParameterRepository.delete(code);
    }
  }
}
