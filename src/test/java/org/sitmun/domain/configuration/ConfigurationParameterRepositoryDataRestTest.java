package org.sitmun.domain.configuration;

import static org.hamcrest.Matchers.is;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.SitmunConstants;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ConfigurationParameter Repository Data REST test")
class ConfigurationParameterRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Value("${sitmun.proxy-middleware.url}")
  private String springProxyUrl;

  @Test
  @DisplayName("GET: Retrieve all ConfigurationParameters")
  void retrieveAll() throws Exception {
    mvc.perform(
            get(URIConstants.CONFIGURATION_PARAMETERS_URI)
                .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$._embedded.configuration-parameters[?(@.name=='"
                        + SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY
                        + "')].value")
                .value("en"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST/PUT: proxy is stored normalized; blank/invalid becomes Spring default")
  void proxyIsStoredNormalized() throws Exception {
    MvcResult created =
        mvc.perform(
                post(URIConstants.CONFIGURATION_PARAMETERS_URI)
                    .contentType(APPLICATION_JSON)
                    .content(
                        asJsonString(
                            ConfigurationParameter.builder()
                                .name(SitmunConstants.PROXY_CONF_KEY)
                                .value("https://cdn.example.com:443/middleware/")
                                .build())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is(SitmunConstants.PROXY_CONF_KEY)))
            .andExpect(jsonPath("$.value", is("https://cdn.example.com/middleware")))
            .andExpect(
                jsonPath(
                    "$.warnings[0]", is("entity.configurationParameter.warning.proxy-normalized")))
            .andReturn();

    String location = created.getResponse().getHeader("Location");
    try {
      mvc.perform(get(location))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.warnings").doesNotExist());

      mvc.perform(
              put(location)
                  .contentType(APPLICATION_JSON)
                  .content(
                      asJsonString(
                          ConfigurationParameter.builder()
                              .name(SitmunConstants.PROXY_CONF_KEY)
                              .value("not-a-valid-uri")
                              .build())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.value", is(springProxyUrl)))
          .andExpect(
              jsonPath(
                  "$.warnings[0]", is("entity.configurationParameter.warning.proxy-defaulted")));

      mvc.perform(
              put(location)
                  .contentType(APPLICATION_JSON)
                  .content(
                      asJsonString(
                          ConfigurationParameter.builder()
                              .name(SitmunConstants.PROXY_CONF_KEY)
                              .value("")
                              .build())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.value", is(springProxyUrl)))
          .andExpect(
              jsonPath(
                  "$.warnings[0]", is("entity.configurationParameter.warning.proxy-defaulted")));

      mvc.perform(
              put(location)
                  .contentType(APPLICATION_JSON)
                  .content(
                      asJsonString(
                          ConfigurationParameter.builder()
                              .name(SitmunConstants.PROXY_CONF_KEY)
                              .value(springProxyUrl)
                              .build())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.value", is(springProxyUrl)))
          .andExpect(jsonPath("$.warnings").doesNotExist());
    } finally {
      mvc.perform(delete(location)).andExpect(status().isNoContent());
    }
  }
}
