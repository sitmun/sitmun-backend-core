package org.sitmun.plugin.core.repository.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class CodeListValueRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void obtainOriginalVersion() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, "cartographyPermission.type"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 31)].description").value("Cartography group"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 32)].description").value("Background map"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 34)].description").value("Report"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 33)].description").value("Location map"));
  }

  @Test
  public void obtainTranslatedVersionSpa() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER + "&lang=spa", "cartographyPermission.type"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 31)].description").value("Grupo de cartografía"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 32)].description").value("Mapa de fondo"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 34)].description").value("Informe"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 33)].description").value("Mapa de situación"));
  }

  @Test
  public void filterType() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, "cartographyPermission.type"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(4)))
      .andExpect(jsonPath(
        "$._embedded.codelist-values[?(@.codeListName == 'cartographyPermission.type')]",
        hasSize(4)));
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, "territory.scope"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values.*", hasSize(3)))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.codeListName == 'territory.scope')]",
        hasSize(3)));
  }

  @Test
  public void cantCreateSystemCodeListValue() throws Exception {
    String body = "{\"value\":\"A\", \"codeListName\":\"B\", \"system\":true}";
    mvc.perform(post(URIConstants.CODELIST_VALUES_URI).content(body))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void cantModifySystemCodeListValue() throws Exception {
    String body = "{\"value\":\"A\", \"codeListName\":\"B\", \"system\":false}";
    mvc.perform(put(URIConstants.CODELIST_VALUE_URI, 31).content(body))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void cantDeleteCodeListValue() throws Exception {
    mvc.perform(delete(URIConstants.CODELIST_VALUE_URI, 31))
      .andExpect(status().isBadRequest());
  }

}
