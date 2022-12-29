package org.sitmun.common.types.codelist;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.common.def.CodeListsConstants.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE;
import static org.sitmun.common.def.CodeListsConstants.DATABASE_CONNECTION_DRIVER;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CodeListValueRepository Data REST test")
class CodeListValueRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("Check availability of databaseConnection.driver")
  @Disabled("Depends on real configuration")
  void obtainDrivers() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, DATABASE_CONNECTION_DRIVER)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values[*].value",
        containsInAnyOrder("org.h2.Driver", "oracle.jdbc.OracleDriver", "org.postgresql.Driver")));
  }

  @Test
  @DisplayName("GET: Description in the original language")
  void obtainOriginalVersion() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, "cartographyPermission.type")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 30)].description").value("Cartography group"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 31)].description").value("Background map"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 32)].description").value("Report"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 33)].description").value("Location map"));

    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, "userPosition.type")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 88)].description").value("City Council"));
  }

  @Test
  @DisplayName("GET: Description translated in ES")
  void obtainTranslatedVersionSpa() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER + "&lang=es", "cartographyPermission.type")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 30)].description").value("Grupo de cartografía"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 31)].description").value("Mapa de fondo"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 32)].description").value("Informe"))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 33)].description").value("Mapa de situación"));
  }

  @Test
  @DisplayName("GET: Description translated in CA")
  void obtainTranslatedVersionCat() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER + "&lang=ca", "userPosition.type")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 88)].description").value("Ajuntament"));
  }

  @Test
  @DisplayName("GET: Filter by code list")
  void filterType() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, "cartographyPermission.type")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.*.*", hasSize(4)))
      .andExpect(jsonPath(
        "$._embedded.codelist-values[?(@.codeListName == 'cartographyPermission.type')]",
        hasSize(4)));
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER, "territory.scope")
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.codelist-values.*", hasSize(3)))
      .andExpect(jsonPath("$._embedded.codelist-values[?(@.codeListName == 'territory.scope')]",
        hasSize(3)));
  }

  /**
   * Check carefully code translations.
   *
   * @see <a href="https://github.com/sitmun/sitmun-backend-core/issues/122#issuecomment-888841191">Issue #122</a>
   */
  @Test
  @DisplayName("GET: Check translated descriptions")
  void checkMangledValues() throws Exception {
    mvc.perform(get(URIConstants.CODELIST_VALUES_URI_FILTER + "&lang=ca", CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath(
        "$._embedded.codelist-values[?(@.id == 23)].value").value("EDIT"))
      .andExpect(jsonPath(
        "$._embedded.codelist-values[?(@.id == 23)].description").value("EDIT"));
  }

  @Test
  @DisplayName("POST: System codes can't be created")
  void cantCreateSystemCodeListValue() throws Exception {
    String body = "{\"value\":\"A\", \"codeListName\":\"B\", \"system\":true}";
    mvc.perform(MockMvcRequestBuilders.post(URIConstants.CODELIST_VALUES_URI)
        .content(body)
        .with(user(Fixtures.admin())))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT: Normal codes can be updated")
  void cantModifySystemCodeListValue() throws Exception {
    String body = "{\"value\":\"A\", \"codeListName\":\"B\", \"system\":false}";
    mvc.perform(MockMvcRequestBuilders.put(URIConstants.CODELIST_VALUE_URI, 31)
        .content(body)
        .with(user(Fixtures.admin())))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("DELETE: System codes can't be deleted")
  void cantDeleteCodeListValue() throws Exception {
    mvc.perform(MockMvcRequestBuilders.delete(URIConstants.CODELIST_VALUE_URI, 31)
        .with(user(Fixtures.admin())))
      .andExpect(status().isBadRequest());
  }

}
