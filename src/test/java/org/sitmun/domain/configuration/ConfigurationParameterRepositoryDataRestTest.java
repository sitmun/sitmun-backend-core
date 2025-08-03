package org.sitmun.domain.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ConfigurationParameter Repository Data REST test")
class ConfigurationParameterRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("GET: Retrieve all ConfigurationParameters")
  void retrieveAll() throws Exception {
    mvc.perform(
            get(URIConstants.CONFIGURATION_PARAMETERS_URI)
                .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$._embedded.configuration-parameters[?(@.name=='language.default')].value")
                .value("en"));
  }
}
