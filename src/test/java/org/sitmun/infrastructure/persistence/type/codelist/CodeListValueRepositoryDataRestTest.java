package org.sitmun.infrastructure.persistence.type.codelist;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.domain.CodeListsConstants.CARTOGRAPHY_PARAMETER_FORMAT;
import static org.sitmun.domain.CodeListsConstants.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE;
import static org.sitmun.domain.CodeListsConstants.DATABASE_CONNECTION_DRIVER;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CodeListValueRepository Data REST test")
class CodeListValueRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Check availability of databaseConnection.driver")
  @WithMockUser(roles = "ADMIN")
  void checkDatabaseConnectionDriverAvailability() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER, DATABASE_CONNECTION_DRIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.codelist-values[*].value", hasItem("org.h2.Driver")));
  }

  @Test
  @DisplayName("GET: Description in the original language")
  @WithMockUser(roles = "ADMIN")
  void obtainOriginalVersion() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER, "cartographyPermission.type"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 24)].description")
                .value("Background map"))
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 25)].description")
                .value("Cartography group"))
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 26)].description")
                .value("Location map"))
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 27)].description").value("Report"));

    mvc.perform(get(CODELIST_VALUES_URI_FILTER, "userPosition.type"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 63)].description")
                .value("City Council"));
  }

  @Test
  @DisplayName("GET: Description translated in ES")
  @WithMockUser(roles = "ADMIN")
  void obtainTranslatedVersionSpa() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER + "&lang=es", "cartographyFilter.type"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 12)].description")
                .value("Personalizado"));
  }

  @Test
  @DisplayName("GET: Description translated in CA")
  @WithMockUser(roles = "ADMIN")
  void obtainTranslatedVersionCat() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER + "&lang=ca", "cartographyFilter.type"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 12)].description")
                .value("Personalitzat"));
  }

  /**
   * Regression: GET with lang and projection=view must not trigger ResultSet closed (PostgreSQL).
   * Request-scoped translation cache is preloaded in interceptor so @PostLoad does not run nested
   * queries.
   */
  @Test
  @DisplayName("GET: cartographyParameter.format with lang=ca and projection=view returns 200")
  @WithMockUser(roles = "ADMIN")
  void codelistValuesWithLangAndProjectionView() throws Exception {
    mvc.perform(
            get(
                CODELIST_VALUES_URI_FILTER + "&lang=ca&projection=view",
                CARTOGRAPHY_PARAMETER_FORMAT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.codelist-values").isArray())
        .andExpect(
            jsonPath(
                    "$._embedded.codelist-values[?(@.codeListName == 'cartographyParameter.format')]")
                .isArray());
  }

  @Test
  @DisplayName("GET: Filter by code list")
  @WithMockUser(roles = "ADMIN")
  void filterType() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER, "cartographyPermission.type"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.*.*", hasSize(4)))
        .andExpect(
            jsonPath(
                "$._embedded.codelist-values[?(@.codeListName == 'cartographyPermission.type')]",
                hasSize(4)));
    mvc.perform(get(CODELIST_VALUES_URI_FILTER, "territory.scope"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.codelist-values.*", hasSize(3)))
        .andExpect(
            jsonPath(
                "$._embedded.codelist-values[?(@.codeListName == 'territory.scope')]", hasSize(3)));
  }

  /**
   * Check code list value and description are returned (no mangling).
   *
   * @see <a
   *     href="https://github.com/sitmun/sitmun-backend-core/issues/122#issuecomment-888841191">Issue
   *     #122</a>
   */
  @Test
  @DisplayName("GET: Check translated descriptions")
  @WithMockUser(roles = "ADMIN")
  void checkMangledValues() throws Exception {
    mvc.perform(
            get(
                CODELIST_VALUES_URI_FILTER + "&lang=ca",
                CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.codelist-values[?(@.id == 34)].value").value("EDIT"))
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 34)].description").value("EDIT"));
  }

  @Test
  @DisplayName("POST: System codes can't be created")
  @WithMockUser(roles = "ADMIN")
  void cantCreateSystemCodeListValue() throws Exception {
    String body =
        """
      {
      "value":"A",
      "codeListName":"B",
      "system":true
      }""";
    mvc.perform(post(CODELIST_VALUES_URI).content(body)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT: System code descriptions can be updated")
  @WithMockUser(roles = "ADMIN")
  void systemCodeDescriptionCanBeUpdated() throws Exception {
    String updatedDescription = "Background map (updated)";
    String body =
        """
      {
      "value":"F",
      "codeListName":"cartographyPermission.type",
      "system":true,
      "defaultCode":false,
      "description":"%s"
      }"""
            .formatted(updatedDescription);
    mvc.perform(put(CODELIST_VALUE_URI, 24).contentType(APPLICATION_JSON).content(body))
        .andExpect(status().is2xxSuccessful());

    mvc.perform(get(CODELIST_VALUES_URI_FILTER, "cartographyPermission.type"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.id == 24)].description")
                .value(updatedDescription));

    // Restore original description so seed data is unchanged
    String restoreBody =
        """
      {
      "value":"F",
      "codeListName":"cartographyPermission.type",
      "system":true,
      "defaultCode":false,
      "description":"Background map"
      }""";
    mvc.perform(put(CODELIST_VALUE_URI, 24).contentType(APPLICATION_JSON).content(restoreBody))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  @DisplayName("PUT: System codes can't be modified (value/codeListName)")
  @WithMockUser(roles = "ADMIN")
  void cantModifySystemCodeListValue() throws Exception {
    String body =
        """
      {
      "value":"A",
      "codeListName":"B",
      "system":false
      }""";
    mvc.perform(put(CODELIST_VALUE_URI, 24).content(body)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("DELETE: System codes can't be deleted")
  @WithMockUser(roles = "ADMIN")
  void cantDeleteCodeListValue() throws Exception {
    mvc.perform(delete(CODELIST_VALUE_URI, 24)).andExpect(status().isBadRequest());
  }
}
