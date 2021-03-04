package org.sitmun.plugin.core.constraints;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("dev")
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
  public void passIfValueIsMin() throws Exception {
    postEntityWithMinMaxValue(VALID_MIN)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_MIN_MAX, equalTo(VALID_MIN)));
  }

  @Test
  public void passIfValueIsMax() throws Exception {
    postEntityWithMinMaxValue(VALID_MAX)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_MIN_MAX, equalTo(VALID_MAX)));
  }

  @Test
  public void passIfValueIsBetween() throws Exception {
    postEntityWithMinMaxValue(VALID_MID)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_MIN_MAX, equalTo(VALID_MID)));
  }

  @Test
  public void failIfEmailValueIsBelowMin() throws Exception {
    postEntityWithMinMaxValue(INVALID_BELOW_MIN)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property", equalTo(PROPERTY_WITH_MIN_MAX)))
      .andExpect(jsonPath("$.errors[0].invalidValue", equalTo(INVALID_BELOW_MIN)));
  }

  @Test
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
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    );
  }

}
