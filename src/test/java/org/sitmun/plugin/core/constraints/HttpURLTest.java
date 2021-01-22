package org.sitmun.plugin.core.constraints;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class HttpURLTest {

  private static final String VALID_HTTP_URL = "http://example.com/somefile";
  private static final String VALID_HTTPS_URL = "https://example.com/somefile";
  private static final String INVALID_URL = "ftp://example.com/somefile";
  private static final String ENTITY_WITH_URL_URI = "http://localhost/api/territories";
  private static final String PROPERTY_WITH_URL = "territorialAuthorityLogo";

  @Autowired
  private MockMvc mvc;

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void passIfURLValueIsHttp() throws Exception {
    postEntityWithEmailValue(VALID_HTTP_URL)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_URL, equalTo(VALID_HTTP_URL)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void passIfURLValueIsHttps() throws Exception {
    postEntityWithEmailValue(VALID_HTTPS_URL)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_URL, equalTo(VALID_HTTPS_URL)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void failIfEmailValueIsWrong() throws Exception {
    postEntityWithEmailValue(INVALID_URL)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property", equalTo(PROPERTY_WITH_URL)))
      .andExpect(jsonPath("$.errors[0].invalidValue", equalTo(INVALID_URL)));
  }

  private ResultActions postEntityWithEmailValue(String validEmail) throws Exception {
    JSONObject entity = new JSONObject()
      .put("name", "Fake Territory")
      .put("code", "0000")
      .put("blocked", false);
    return mvc.perform(post(ENTITY_WITH_URL_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(entity.put(PROPERTY_WITH_URL, validEmail).toString())
    );
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
