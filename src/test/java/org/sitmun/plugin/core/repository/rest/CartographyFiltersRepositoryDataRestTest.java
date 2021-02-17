package org.sitmun.plugin.core.repository.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.test.URIConstants;
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
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.Assert.assertNotNull;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class CartographyFiltersRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void newCartographyFilterCanBePosted() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"type\":\"C\"," +
      "\"required\":true," +
      "\"cartography\":\"http://localhost/api/cartographies/1228\"" +
      "}";

    String location = mvc.perform(
      post(URIConstants.CARTOGRAPHY_FILTERS_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);

    mvc.perform(get(location + "/cartography"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(1228));

    mvc.perform(delete(location)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  public void newCartographyFilterRequiresCartographyLink() throws Exception {
    String content = "{" +
      "\"name\":\"test\"," +
      "\"type\":\"C\"," +
      "\"required\":true" +
      "}";

    mvc.perform(
      post(URIConstants.CARTOGRAPHY_FILTERS_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("cartography"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
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
