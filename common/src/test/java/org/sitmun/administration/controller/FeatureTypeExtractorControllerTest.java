package org.sitmun.administration.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Feature Type extractor controller test")
class FeatureTypeExtractorControllerTest extends BaseTest {

  private static final String URI_TEMPLATE = "/api/helpers/feature-type?url={0}";

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Extract from a DescribeFeatureType request to a WFS 2.0")
  void extractKnownFeatureWFS20() throws Exception {
    mvc.perform(get(URI_TEMPLATE, "https://www.ign.es/wfs/redes-geodesicas?request=DescribeFeatureType&service=WFS&typeNames=RED_REGENTE"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.type").value("GML Schema"))
      .andExpect(jsonPath("$.asText", Matchers.startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.['xsd:schema']").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Extract from a DescribeFeatureType request to a WFS 1.1.0")
  void extractKnownFeatureWFS110() throws Exception {
    mvc.perform(get(URI_TEMPLATE, "https://www.ign.es/wfs/redes-geodesicas?request=DescribeFeatureType&service=WFS&version=1.1.0&typeNames=RED_REGENTE"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.type").value("GML Schema"))
      .andExpect(jsonPath("$.asText", Matchers.startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.['xsd:schema']").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Extract from a bad request to a WFS")
  void extractFailedService() throws Exception {
    mvc.perform(get(URI_TEMPLATE, "https://www.ign.es/wfs/redes-geodesicas?"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Unmanaged XML response"))
      .andExpect(jsonPath("$.asText", Matchers.startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.['ows:ExceptionReport']").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Extract from a request to HTML page")
  void extractHtmlPage() throws Exception {
    mvc.perform(get(URI_TEMPLATE, "https://www.ign.es/"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Unmanaged XML response"))
      .andExpect(jsonPath("$.asText", Matchers.startsWith("<!DOCTYPE")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Extract from a request to a not found page")
  void extract404Page() throws Exception {
    mvc.perform(get(URI_TEMPLATE, "https://www.ign.es/not-found"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Not a well formed XML"))
      .andExpect(jsonPath("$.asText", Matchers.startsWith("<html")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Extract from a request to an nonexistent domain")
  void extractNonExistentDomain() throws Exception {
    mvc.perform(get(URI_TEMPLATE, "https://fake"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason", Matchers.startsWith("UnknownHostException: fake")));
  }
}