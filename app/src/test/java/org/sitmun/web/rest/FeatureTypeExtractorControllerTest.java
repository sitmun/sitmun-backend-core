package org.sitmun.web.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.Application;
import org.sitmun.test.Fixtures;
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
public class FeatureTypeExtractorControllerTest {

  private static final String URI_TEMPLATE = "/api/helpers/feature-type?url={0}";

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("Extract from a DescribeFeatureType request to a WFS 2.0")
  public void extractKnownFeatureWFS20() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE, "https://www.ign.es/wfs/redes-geodesicas?request=DescribeFeatureType&service=WFS&typeNames=RED_REGENTE")
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.type").value("GML Schema"))
      .andExpect(jsonPath("$.asText", startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.['xsd:schema']").exists());
  }

  @Test
  @DisplayName("Extract from a DescribeFeatureType request to a WFS 1.1.0")
  public void extractKnownFeatureWFS110() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE, "https://www.ign.es/wfs/redes-geodesicas?request=DescribeFeatureType&service=WFS&version=1.1.0&typeNames=RED_REGENTE")
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.type").value("GML Schema"))
      .andExpect(jsonPath("$.asText", startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.['xsd:schema']").exists());
  }

  @Test
  @DisplayName("Extract from a bad request to a WFS")
  public void extractFailedService() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/wfs/redes-geodesicas?"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Unmanaged XML response"))
      .andExpect(jsonPath("$.asText", startsWith("<?xml")))
      .andExpect(jsonPath("$.asJson.['ows:ExceptionReport']").exists());
  }

  @Test
  @DisplayName("Extract from a request to HTML page")
  public void extractHtmlPage() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason").value("Unmanaged XML response"))
      .andExpect(jsonPath("$.asText", startsWith("<!DOCTYPE")));
  }

  @Test
  @DisplayName("Extract from a request to a not found page")
  public void extract404Page() throws Exception {
    mockMvc.perform(get(URI_TEMPLATE,
        "https://www.ign.es/not-found"
      )
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
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
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.reason", startsWith("UnknownHostException: fake")));
  }
}