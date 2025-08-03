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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

@DisplayName("SpatialReferenceSystem validation test")
@Import(CodeListValidator.class)
class SpatialReferenceSystemTest extends BaseTest {

  @MockBean private CodeListValueRepository repository;

  @BeforeEach
  void mocks() {
    when(repository.existsByCodeListNameAndValue(any(String.class), any(String.class)))
        .thenReturn(true);
  }

  @Test
  @DisplayName("Single projection pass")
  @WithMockUser(roles = "ADMIN")
  void singleProjectionPass() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("EPSG:1")))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getHeader("Location");
  }

  @Test
  @DisplayName("Single other value fail")
  @WithMockUser(roles = "ADMIN")
  void singleOtherValueFail() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("other")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].property").value("supportedSRS"))
        .andExpect(jsonPath("$.errors[0].invalidValue[0]").value("other"));
  }

  @Test
  @DisplayName("Multiple projections pass")
  @WithMockUser(roles = "ADMIN")
  void multipleProjectionPass() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("EPSG:1", "EPSG:2")))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Multiple projections with other value fail")
  @WithMockUser(roles = "ADMIN")
  void multipleProjectionsWithOtherValueFail() throws Exception {
    mvc.perform(
            post(URIConstants.SERVICES_URI)
                .contentType(APPLICATION_JSON)
                .content(serviceFixture("EPSG:1", "other")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].property").value("supportedSRS"))
        .andExpect(jsonPath("$.errors[0].invalidValue[0]").value("EPSG:1"))
        .andExpect(jsonPath("$.errors[0].invalidValue[1]").value("other"));
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
