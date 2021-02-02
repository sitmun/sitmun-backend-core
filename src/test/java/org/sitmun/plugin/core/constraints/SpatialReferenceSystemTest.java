package org.sitmun.plugin.core.constraints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("dev")
public class SpatialReferenceSystemTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("Single projection pass")
  public void singleProjectionPass() throws Exception {
    String location = mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("EPSG:1"))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Single other value fail")
  public void singleOtherValueFail() throws Exception {
    mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("other")))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("supportedSRS"))
      .andExpect(jsonPath("$.errors[0].invalidValue[0]").value("other"));
  }

  @Test
  @DisplayName("Multiple projections pass")
  public void multipleProjectionPass() throws Exception {
    String location = mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("EPSG:1", "EPSG:2"))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isCreated())
      .andReturn().getResponse().getHeader("Location");
    assertThat(location).isNotNull();
    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Multiple projections with other value fail")
  public void multipleProjectionsWithOtherValueFail() throws Exception {
    mvc.perform(post("/api/services")
      .contentType(APPLICATION_JSON)
      .content(serviceFixture("EPSG:1", "other")))
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

  @TestConfiguration
  static class ContextConfiguration {
    @Bean
    public Validator validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig(validator());
    }
  }
}
