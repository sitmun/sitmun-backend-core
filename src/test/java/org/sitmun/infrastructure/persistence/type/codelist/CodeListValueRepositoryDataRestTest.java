package org.sitmun.infrastructure.persistence.type.codelist;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.domain.CodeListsConstants.CARTOGRAPHY_PARAMETER_FORMAT;
import static org.sitmun.domain.CodeListsConstants.CARTOGRAPHY_PARAMETER_GEOMETRY_KIND;
import static org.sitmun.domain.CodeListsConstants.CARTOGRAPHY_PARAMETER_PREFERRED_GEOMETRY_TYPE;
import static org.sitmun.domain.CodeListsConstants.CARTOGRAPHY_SPATIAL_SELECTION_PARAMETER_TYPE;
import static org.sitmun.domain.CodeListsConstants.DATABASE_CONNECTION_DRIVER;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CodeListValueRepository Data REST test")
class CodeListValueRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("GET: Check availability of databaseConnection.driver")
  @WithMockUser(roles = "ADMIN")
  void checkDatabaseConnectionDriverAvailability() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER, DATABASE_CONNECTION_DRIVER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.codelist-values[*].value", hasItem("org.h2.Driver")))
        .andExpect(
            jsonPath(
                "$._embedded.codelist-values[*].value",
                hasItem("com.microsoft.sqlserver.jdbc.SQLServerDriver")));
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
  @DisplayName("POST of a system code returns 400 and System field cannot be set true")
  @WithMockUser(roles = "ADMIN")
  void cantCreateSystemCodeListValue() throws Exception {
    String body =
        """
      {
      "value":"A",
      "codeListName":"B",
      "system":true
      }""";
    mvc.perform(post(CODELIST_VALUES_URI).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("System field cannot be set true"));
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
  @DisplayName("PUT of a system code rejects a system flip, a new value, and a new code list name")
  @WithMockUser(roles = "ADMIN")
  void cantModifySystemCodeListValue() throws Exception {
    mvc.perform(
            put(CODELIST_VALUE_URI, 24)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                    "value":"F",
                    "codeListName":"cartographyPermission.type",
                    "system":false
                    }"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("System field cannot be changed"));

    mvc.perform(
            put(CODELIST_VALUE_URI, 24)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                    "value":"A",
                    "codeListName":"cartographyPermission.type",
                    "system":true
                    }"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Value cannot be changed in system property"));

    mvc.perform(
            put(CODELIST_VALUE_URI, 24)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                    "value":"F",
                    "codeListName":"B",
                    "system":true
                    }"""))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail").value("Code list name cannot be changed in system property"));
  }

  @Test
  @DisplayName("DELETE of a system code returns 400 and System field cannot be deleted")
  @WithMockUser(roles = "ADMIN")
  void cantDeleteCodeListValue() throws Exception {
    mvc.perform(delete(CODELIST_VALUE_URI, 24))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("System field cannot be deleted"));

    mvc.perform(get(CODELIST_VALUE_URI, 24)).andExpect(status().isOk());
  }

  /**
   * Regression for https://github.com/sitmun/sitmun-admin-app/issues/411.
   *
   * <p>When the connection form requests databaseConnection.driver with lang=es, each driver must
   * show its own JDBC driver label, not an unrelated translation (e.g. GIF, JPEG, PNG) caused by a
   * COD_ID collision between startup-created rows and hardcoded translation rows from migration 44.
   */
  @Test
  @DisplayName("GET databaseConnection.driver descriptions in Spanish")
  @WithMockUser(roles = "ADMIN")
  void driverCodelistTranslationsCorrectInSpanish() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER + "&lang=es", DATABASE_CONNECTION_DRIVER))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.value == 'org.h2.Driver')].description")
                .value("Controlador JDBC H2"))
        .andExpect(
            jsonPath(
                    "$._embedded.codelist-values[?(@.value == 'org.postgresql.Driver')].description")
                .value("Controlador JDBC PostgreSQL"))
        .andExpect(
            jsonPath(
                    "$._embedded.codelist-values[?(@.value == 'oracle.jdbc.OracleDriver')].description")
                .value("Controlador JDBC Oracle"))
        .andExpect(
            jsonPath(
                    "$._embedded.codelist-values[?(@.value == 'com.microsoft.sqlserver.jdbc.SQLServerDriver')].description")
                .value("Controlador JDBC Microsoft SQL Server"));
  }

  /**
   * Regression for https://github.com/sitmun/sitmun-admin-app/issues/411.
   *
   * <p>Same check in Catalan (lang=ca).
   */
  @Test
  @DisplayName("GET databaseConnection.driver descriptions in Catalan")
  @WithMockUser(roles = "ADMIN")
  void driverCodelistTranslationsCorrectInCatalan() throws Exception {
    mvc.perform(get(CODELIST_VALUES_URI_FILTER + "&lang=ca", DATABASE_CONNECTION_DRIVER))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.codelist-values[?(@.value == 'org.h2.Driver')].description")
                .value("Controlador JDBC H2"))
        .andExpect(
            jsonPath(
                    "$._embedded.codelist-values[?(@.value == 'org.postgresql.Driver')].description")
                .value("Controlador JDBC PostgreSQL"))
        .andExpect(
            jsonPath(
                    "$._embedded.codelist-values[?(@.value == 'oracle.jdbc.OracleDriver')].description")
                .value("Controlador JDBC Oracle"))
        .andExpect(
            jsonPath(
                    "$._embedded.codelist-values[?(@.value == 'com.microsoft.sqlserver.jdbc.SQLServerDriver')].description")
                .value("Controlador JDBC Microsoft SQL Server"));
  }

  @Test
  @DisplayName("GET cartographyParameter.geometryKind descriptions in es, ca, oc-aranes, and fr")
  @WithMockUser(roles = "ADMIN")
  void geometryKindDescriptionsInSpanishAndCatalan() throws Exception {
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_GEOMETRY_KIND,
        "es",
        Map.ofEntries(
            Map.entry("Point", "Punto"),
            Map.entry("MultiPoint", "Multipunto"),
            Map.entry("LineString", "Línea"),
            Map.entry("MultiLineString", "Multilínea"),
            Map.entry("Curve", "Curva"),
            Map.entry("MultiCurve", "Multicurva"),
            Map.entry("Polygon", "Polígono"),
            Map.entry("MultiPolygon", "Multipolígono"),
            Map.entry("Surface", "Superficie"),
            Map.entry("MultiSurface", "Multisuperficie"),
            Map.entry("Geometry", "Geometría")));
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_GEOMETRY_KIND,
        "ca",
        Map.ofEntries(
            Map.entry("Point", "Punt"),
            Map.entry("MultiPoint", "Multipunt"),
            Map.entry("LineString", "Línia"),
            Map.entry("MultiLineString", "Multilínia"),
            Map.entry("Curve", "Corba"),
            Map.entry("MultiCurve", "Multicorba"),
            Map.entry("Polygon", "Polígon"),
            Map.entry("MultiPolygon", "Multipolígon"),
            Map.entry("Surface", "Superfície"),
            Map.entry("MultiSurface", "Multisuperfície"),
            Map.entry("Geometry", "Geometria")));
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_GEOMETRY_KIND,
        "oc-aranes",
        Map.ofEntries(
            Map.entry("Point", "Punt"),
            Map.entry("MultiPoint", "Multipunt"),
            Map.entry("LineString", "Linha"),
            Map.entry("MultiLineString", "Multilinha"),
            Map.entry("Curve", "Corba"),
            Map.entry("MultiCurve", "Multicorba"),
            Map.entry("Polygon", "Poligòn"),
            Map.entry("MultiPolygon", "Multipoligòn"),
            Map.entry("Surface", "Superfícia"),
            Map.entry("MultiSurface", "Multisuperfícia"),
            Map.entry("Geometry", "Geometria")));
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_GEOMETRY_KIND,
        "fr",
        Map.ofEntries(
            Map.entry("Point", "Point"),
            Map.entry("MultiPoint", "Multipoint"),
            Map.entry("LineString", "Ligne"),
            Map.entry("MultiLineString", "Multiligne"),
            Map.entry("Curve", "Courbe"),
            Map.entry("MultiCurve", "Multicourbe"),
            Map.entry("Polygon", "Polygone"),
            Map.entry("MultiPolygon", "Multipolygone"),
            Map.entry("Surface", "Surface"),
            Map.entry("MultiSurface", "Multisurface"),
            Map.entry("Geometry", "Géométrie")));
  }

  @Test
  @DisplayName(
      "GET cartographyParameter.preferredGeometryType descriptions in es, ca, oc-aranes, and fr")
  @WithMockUser(roles = "ADMIN")
  void preferredGeometryTypeDescriptionsInSpanishAndCatalan() throws Exception {
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_PREFERRED_GEOMETRY_TYPE,
        "es",
        Map.of(
            "point", "Punto",
            "multipoint", "Multipunto",
            "polyline", "Polilínea",
            "polygon", "Polígono",
            "multipolyline", "Multipolilínea",
            "multipolygon", "Multipolígono"));
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_PREFERRED_GEOMETRY_TYPE,
        "ca",
        Map.of(
            "point", "Punt",
            "multipoint", "Multipunt",
            "polyline", "Polilínia",
            "polygon", "Polígon",
            "multipolyline", "Multipolilínia",
            "multipolygon", "Multipolígon"));
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_PREFERRED_GEOMETRY_TYPE,
        "oc-aranes",
        Map.of(
            "point", "Punt",
            "multipoint", "Multipunt",
            "polyline", "Polilinha",
            "polygon", "Poligòn",
            "multipolyline", "Multipolilinha",
            "multipolygon", "Multipoligòn"));
    expectDescriptions(
        CARTOGRAPHY_PARAMETER_PREFERRED_GEOMETRY_TYPE,
        "fr",
        Map.of(
            "point", "Point",
            "multipoint", "Multipoint",
            "polyline", "Polyligne",
            "polygon", "Polygone",
            "multipolyline", "Multipolyligne",
            "multipolygon", "Multipolygone"));
  }

  @Test
  @DisplayName("GET cartographyParameter.format Geometry description in es, ca, oc-aranes, and fr")
  @WithMockUser(roles = "ADMIN")
  void geometryFormatDescriptionInSpanishAndCatalan() throws Exception {
    expectDescriptions(CARTOGRAPHY_PARAMETER_FORMAT, "es", Map.of("Geometry", "Geometría"));
    expectDescriptions(CARTOGRAPHY_PARAMETER_FORMAT, "ca", Map.of("Geometry", "Geometria"));
    expectDescriptions(CARTOGRAPHY_PARAMETER_FORMAT, "oc-aranes", Map.of("Geometry", "Geometria"));
    expectDescriptions(CARTOGRAPHY_PARAMETER_FORMAT, "fr", Map.of("Geometry", "Géométrie"));
  }

  private void expectDescriptions(String codeListName, String lang, Map<String, String> expected)
      throws Exception {
    // A previous request in this transaction already translated the managed rows.
    entityManager.clear();
    var result =
        mvc.perform(get(CODELIST_VALUES_URI_FILTER + "&lang=" + lang, codeListName))
            .andExpect(status().isOk());
    for (Map.Entry<String, String> entry : expected.entrySet()) {
      result.andExpect(
          jsonPath(
                  "$._embedded.codelist-values[?(@.value == '%s')].description"
                      .formatted(entry.getKey()))
              .value(entry.getValue()));
    }
  }
}
