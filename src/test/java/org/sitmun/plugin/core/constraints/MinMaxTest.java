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
public class MinMaxTest {

  private static final Integer VALID_MIN = 0;
  private static final Integer VALID_MID = 50;
  private static final Integer VALID_MAX = 100;
  private static final Integer INVALID_BELOW_MIN = -1;
  private static final Integer INVALID_UPPER_MAX = 101;
  private static final String ENTITY_WITH_MIN_MAX_URI = "http://localhost/api/thematic-maps";
  private static final String PROPERTY_WITH_MIN_MAX = "transparency";

  @Autowired
  private MockMvc mvc;

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void passIfValueIsMin() throws Exception {
    postEntityWithMinMaxValue(VALID_MIN)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_MIN_MAX, equalTo(VALID_MIN)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void passIfValueIsMax() throws Exception {
    postEntityWithMinMaxValue(VALID_MAX)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_MIN_MAX, equalTo(VALID_MAX)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void passIfValueIsBetween() throws Exception {
    postEntityWithMinMaxValue(VALID_MID)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_MIN_MAX, equalTo(VALID_MID)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void failIfEmailValueIsBelowMin() throws Exception {
    postEntityWithMinMaxValue(INVALID_BELOW_MIN)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property", equalTo(PROPERTY_WITH_MIN_MAX)))
      .andExpect(jsonPath("$.errors[0].invalidValue", equalTo(INVALID_BELOW_MIN)));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void failIfEmailValueIsUpperMax() throws Exception {
    postEntityWithMinMaxValue(INVALID_UPPER_MAX)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property", equalTo(PROPERTY_WITH_MIN_MAX)))
      .andExpect(jsonPath("$.errors[0].invalidValue", equalTo(INVALID_UPPER_MAX)));
  }

  private ResultActions postEntityWithMinMaxValue(Integer value) throws Exception {
    JSONObject entity = new JSONObject().put("id", null);
    return mvc.perform(post(ENTITY_WITH_MIN_MAX_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(entity.put(PROPERTY_WITH_MIN_MAX, value).toString())
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
