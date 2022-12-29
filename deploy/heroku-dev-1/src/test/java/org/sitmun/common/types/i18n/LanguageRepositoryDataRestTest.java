package org.sitmun.common.types.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class LanguageRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void obtainOriginalVersion() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.languages[?(@.shortname == 'en')].name").value("English"));
  }

  @Test
  void obtainTranslatedVersionSpa() throws Exception {
    mvc.perform(get(URIConstants.LANGUAGES_URI + "?lang=es"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.languages[?(@.shortname == 'en')].name").value("Inglés"));
  }

}
