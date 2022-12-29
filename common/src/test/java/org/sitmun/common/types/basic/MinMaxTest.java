package org.sitmun.common.types.basic;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.test.URIConstants.CARTOGRAPHIES_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MinMax validation test")
class MinMaxTest extends BaseTest {

  private static final Integer VALID_VALUE = 50;
  private static final Integer INVALID_BELOW_MIN = -1;
  private static final Integer INVALID_UPPER_MAX = 101;
  private static final String ENTITY_WITH_MIN_MAX_URI = CARTOGRAPHIES_URI;
  private static final String PROPERTY_WITH_MIN_MAX = "transparency";

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Fail if value is below min")
  void failIfValueIsBelowMin() throws Exception {
    postEntityWithMinMaxValue(INVALID_BELOW_MIN)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[?(@.property=='" + PROPERTY_WITH_MIN_MAX + "')].invalidValue", hasItem(INVALID_BELOW_MIN)));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Fail if value is above max")
  void failIfValueIsAboveMax() throws Exception {
    postEntityWithMinMaxValue(INVALID_UPPER_MAX)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[?(@.property=='" + PROPERTY_WITH_MIN_MAX + "')].invalidValue", hasItem(INVALID_UPPER_MAX)));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Pass if value is in range")
  void passIfValueIsInRange() throws Exception {
    postEntityWithMinMaxValue(VALID_VALUE)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[?(@.property=='" + PROPERTY_WITH_MIN_MAX + "')]", hasSize(0)));
  }

  private ResultActions postEntityWithMinMaxValue(Integer value) throws Exception {
    JSONObject entity = new JSONObject()
      .put("id", (Integer) null);
    return mvc.perform(post(ENTITY_WITH_MIN_MAX_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(entity.put(PROPERTY_WITH_MIN_MAX, value).toString())
    );
  }

}
