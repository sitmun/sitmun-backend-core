package org.sitmun.infrastructure.persistence.type.srs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValidator;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValueRepository;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("SpatialReferenceSystem validation test")
@Import(CodeListValidator.class)
class SpatialReferenceSystemTest extends BaseTest {

  @MockitoBean private CodeListValueRepository repository;

  @BeforeEach
  void mocks() {
    when(repository.existsByCodeListNameAndValue(any(String.class), any(String.class)))
        .thenReturn(true);
  }

  @Test
  @DisplayName("POST stores a single EPSG code")
  @WithMockUser(roles = "ADMIN")
  void postStoresASingleEpsgCode() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("EPSG:1")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.supportedSRS[0]").value("EPSG:1"));
  }

  @Test
  @DisplayName("POST rejects a non-EPSG code")
  @WithMockUser(roles = "ADMIN")
  void postRejectsNonEpsgCode() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("other")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("supportedSRS"))
        .andExpect(jsonPath("$.errors[0].rejectedValue[0]").value("other"));
  }

  @Test
  @DisplayName("POST stores two EPSG codes")
  @WithMockUser(roles = "ADMIN")
  void postStoresTwoEpsgCodes() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("EPSG:1", "EPSG:2")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.supportedSRS[0]").value("EPSG:1"))
        .andExpect(jsonPath("$.supportedSRS[1]").value("EPSG:2"));
  }

  @Test
  @DisplayName("POST rejects a mix of EPSG and a non-EPSG code")
  @WithMockUser(roles = "ADMIN")
  void postRejectsMixedEpsgAndOtherCode() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("EPSG:1", "other")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("supportedSRS"))
        .andExpect(jsonPath("$.errors[0].rejectedValue[0]").value("EPSG:1"))
        .andExpect(jsonPath("$.errors[0].rejectedValue[1]").value("other"));
  }

  String serviceFixture(String... projection) throws JSONException {
    return new JSONObject()
        .put("supportedSRS", new JSONArray(projection))
        .put("type", "WMS")
        .put("blocked", "false")
        .put("name", "any name")
        .put("serviceURL", "http://example.com/")
        .toString();
  }
}
