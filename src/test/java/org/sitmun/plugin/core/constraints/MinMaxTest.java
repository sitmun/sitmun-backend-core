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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.URIConstants.CARTOGRAPHIES_URI;
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
  private static final String ENTITY_WITH_MIN_MAX_URI = CARTOGRAPHIES_URI;
  private static final String PROPERTY_WITH_MIN_MAX = "transparency";

  @Autowired
  private MockMvc mvc;

  @Test
  public void failIfEmailValueIsBelowMin() throws Exception {
    postEntityWithMinMaxValue(INVALID_BELOW_MIN)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[?(@.property=='" + PROPERTY_WITH_MIN_MAX + "')].invalidValue", hasItem(INVALID_BELOW_MIN)));
  }

  @Test
  public void failIfEmailValueIsUpperMax() throws Exception {
    postEntityWithMinMaxValue(INVALID_UPPER_MAX)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[?(@.property=='" + PROPERTY_WITH_MIN_MAX + "')].invalidValue", hasItem(INVALID_UPPER_MAX)));
  }

  private ResultActions postEntityWithMinMaxValue(Integer value) throws Exception {
    JSONObject entity = new JSONObject()
      .put("id", null);
    return mvc.perform(post(ENTITY_WITH_MIN_MAX_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(entity.put(PROPERTY_WITH_MIN_MAX, value).toString())
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    );
  }

}
