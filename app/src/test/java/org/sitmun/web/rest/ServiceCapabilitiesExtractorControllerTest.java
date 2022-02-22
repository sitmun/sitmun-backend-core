package org.sitmun.web.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.Application;
import org.sitmun.test.TestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ServiceCapabilitiesExtractorControllerTest {

  private static final String URI_TEMPLATE = "/api/helpers/capabilities?url={0}";

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Extract from a GetCapabilities request to a WMS 1.3.0")
  public void extractKnownWMSService130() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/wms-inspire/ign-base?request=GetCapabilities"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.type").value("OGC:WMS 1.3.0"))
      .andExpect(jsonPath("$.asText", startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.WMS_Capabilities").exists());
  }

  @Test
  @DisplayName("Extract from a GetCapabilities request to a WMS 1.1.1")
  public void extractKnownWMSService111() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/wms-inspire/ign-base?request=GetCapabilities&version=1.1.1"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.type").value("OGC:WMS 1.1.1"))
      .andExpect(jsonPath("$.asText", startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.WMT_MS_Capabilities").exists());
  }

  @Test
  @DisplayName("Extract from a bad request to a WMS")
  public void extractFailedService() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/wms-inspire/ign-base?"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Not a standard OGC:WMS Capabilities response"))
      .andExpect(jsonPath("$.asText", startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.ServiceExceptionReport").exists());
  }

  @Test
  @DisplayName("Extract from a request to HTML page")
  public void extractHtmlPage() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Not a standard OGC:WMS Capabilities response"))
      .andExpect(jsonPath("$.asText", startsWith("<!DOCTYPE")));
  }

  @Test
  @DisplayName("Extract from a request to a not found page")
  public void extract404Page() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/not-found"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Not a well formed XML"))
      .andExpect(jsonPath("$.asText", startsWith("<html")));
  }

  @Test
  @DisplayName("Extract from a request to an nonexistent domain")
  public void extractNonExistentDomain() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://fake"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(TestConstants.SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason", startsWith("UnknownHostException: fake")));
  }
}