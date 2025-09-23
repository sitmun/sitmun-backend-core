package org.sitmun.infrastructure.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LanguageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LanguageController.class)
@DisplayName("LanguageController REST test")
@AutoConfigureMockMvc(addFilters = false)
class LanguageControllerTest {

  @Autowired private MockMvc mvc;

  @MockitoBean
  private LanguageRepository languageRepository;

  @Test
  @DisplayName("GET: Retrieve all languages - OK")
  void retrieveAllLanguages() throws Exception {
    Language lang1 = new Language();
    lang1.setId(1);
    lang1.setName("English");
    lang1.setShortname("en");

    Language lang2 = new Language();
    lang2.setId(2);
    lang2.setName("Espagnol");
    lang2.setShortname("en");

    when(languageRepository.findAll()).thenReturn(Arrays.asList(lang1, lang2));

    mvc.perform(get("/api/config/languages"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(1))
      .andExpect(jsonPath("$[0].name").value("English"))
      .andExpect(jsonPath("$[0].shortName").value("en"))
      .andExpect(jsonPath("$[1].id").value(2))
      .andExpect(jsonPath("$[1].name").value("Espagnol"))
      .andExpect(jsonPath("$[1].shortName").value("en"));
  }

  @Test
  @DisplayName("GET: Retrieve all languages - Not Found when empty")
  void retrieveNoLanguages() throws Exception {
    when(languageRepository.findAll()).thenReturn(Collections.emptyList());

    mvc.perform(get("/api/config/languages"))
      .andExpect(status().isNotFound());
  }
}
