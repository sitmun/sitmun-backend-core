package org.sitmun.constraints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.Fixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional

public class SpatialReferenceSystemTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("Single projection pass")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void singleProjectionPass() throws Exception {
    String location = mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("EPSG:1"))
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Single other value fail")
  public void singleOtherValueFail() throws Exception {
    mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("other"))
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("supportedSRS"))
      .andExpect(jsonPath("$.errors[0].invalidValue[0]").value("other"));
  }

  @Test
  @DisplayName("Multiple projections pass")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void multipleProjectionPass() throws Exception {
    String location = mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("EPSG:1", "EPSG:2"))
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Multiple projections with other value fail")
  public void multipleProjectionsWithOtherValueFail() throws Exception {
    mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("EPSG:1", "other"))
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("supportedSRS"))
      .andExpect(jsonPath("$.errors[0].invalidValue[0]").value("EPSG:1"))
      .andExpect(jsonPath("$.errors[0].invalidValue[1]").value("other"));
  }

  public String serviceFixture(String... projection) throws JSONException {
    return new JSONObject()
      .put("supportedSRS", new JSONArray(projection))
      .put("type", "WMS")
      .put("blocked", "false")
      .put("name", "any name")
      .put("serviceURL", "http://example.com/")
      .toString();
  }

}
