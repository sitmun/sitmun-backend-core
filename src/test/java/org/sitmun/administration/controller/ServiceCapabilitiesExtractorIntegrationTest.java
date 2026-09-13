package org.sitmun.administration.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@DisplayName("Service Capabilities Extractor integration test")
class ServiceCapabilitiesExtractorIntegrationTest extends BaseTest {

  private static final String CAPABILITIES_URI = "/api/helpers/capabilities";

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET leftovers are gone")
  void getIsNotSupported() throws Exception {
    mvc.perform(get(CAPABILITIES_URI).param("url", "https://example.com/wms"))
        .andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Non-WMS type fails before origin fetch")
  void rejectNonWmsType() throws Exception {
    postCapabilities(
            new JSONObject().put("url", "https://example.com/wmts").put("type", "WMTS").toString())
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.reason").value("Unsupported service type for capabilities: WMTS"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("WMS GetCapabilties: A request with a percent-encoded ampersand succeeds")
  void usePercentEncodedAmpersand() throws Exception {
    postCapabilities(
            body(
                "https://sitmun.diba.cat/wms/servlet/ACE1M?request=GetCapabilities%26service=WMS",
                "WMS"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.type").value("OGC:WMS 1.3.0"))
        .andExpect(jsonPath("$.asText", startsWith("<?xml")))
        .andExpect(jsonPath("$.asJson.WMS_Capabilities").isMap())
        .andExpect(jsonPath("$.asJson.WMS_Capabilities").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("WMS GetCapabilties: Extract from a GetCapabilities request to a WMS 1.3.0")
  void extractKnownWMSService130() throws Exception {
    postCapabilities(body("https://www.ign.es/wms-inspire/ign-base?request=GetCapabilities", "WMS"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.type").value("OGC:WMS 1.3.0"))
        .andExpect(jsonPath("$.asText", startsWith("<?xml")))
        .andExpect(jsonPath("$.asJson.WMS_Capabilities").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("WMS GetCapabilties: Extract from a GetCapabilities request to a WMS 1.1.1")
  void extractKnownWMSService111() throws Exception {
    postCapabilities(
            body(
                "https://www.ign.es/wms-inspire/ign-base?request=GetCapabilities&version=1.1.1",
                "WMS"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.type").value("OGC:WMS 1.1.1"))
        .andExpect(jsonPath("$.asText", startsWith("<?xml")))
        .andExpect(jsonPath("$.asJson.WMT_MS_Capabilities").exists());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("HTML: Extract from a request to HTML page")
  void extractHtmlPage() throws Exception {
    postCapabilities(body("https://www.ign.es/", "WMS"))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.reason").value("Not a standard OGC:WMS Capabilities response"))
        .andExpect(jsonPath("$.asText", startsWith("<!DOCTYPE")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("HTML: Extract from a request to a not found page")
  void extract404Page() throws Exception {
    postCapabilities(body("https://www.ign.es/not-found", "WMS"))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.reason").value("Not a well formed XML"))
        .andExpect(jsonPath("$.asText", startsWith("<html")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DNS: Extract from a request to an nonexistent domain")
  void extractNonExistentDomain() throws Exception {
    postCapabilities(body("https://fake", "WMS"))
        .andExpect(MockMvcResultMatchers.status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.reason", startsWith("UnknownHostException: fake")));
  }

  private ResultActions postCapabilities(String json) throws Exception {
    return mvc.perform(
        post(CAPABILITIES_URI).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  private static String body(String url, String type) {
    return new JSONObject().put("url", url).put("type", type).toString();
  }
}
