package org.sitmun.plugin.core.repository.rest;

import org.junit.Test;
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
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.util.UriTemplate;

import static org.junit.Assert.assertNotNull;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class CartographyParametersRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  public void newCartographyParametersCanBePosted() throws Exception {
    String content = "{\"value\":\"test \",\"name\":\"test\",\"format\":\"Imagen\",\"order\":null,\"type\":\"INFO\",\"status\":\"Pending creation\", \"cartography\":\"http://localhost/api/cartographies/0\"}";

    String location = mvc.perform(
      post("/api/cartography-parameters")
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);
    String id = new UriTemplate("http://localhost/api/cartography-parameters/{id}").match(location).get("id");

    mvc.perform(get("/api/cartography-parameters/{id}/cartography", id))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(0));

    mvc.perform(delete("/api/cartography-parameters/{id}", id)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  public void newCartographyParameterRequiresCartographyLink() throws Exception {
    String content = "{\"value\":\"test \",\"name\":\"test\",\"format\":\"Imagen\",\"order\":null,\"type\":\"INFO\",\"status\":\"Pending creation\"}";

    mvc.perform(
      post("/api/cartography-parameters")
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
