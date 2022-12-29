package org.sitmun.common.types.basic;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Email validation test")
class EmailTest extends BaseTest {

  private static final String VALID_EMAIL = "fake@example.com";
  private static final String INVALID_EMAIL = "fake @ example.com";
  private static final String ENTITY_WITH_EMAIL_URI = URIConstants.TERRITORIES_URI;
  private static final String PROPERTY_WITH_EMAIL = "territorialAuthorityEmail";

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Pass if email value is valid")
  void passIfEmailValueIsValid() throws Exception {
    postEntityWithEmailValue(VALID_EMAIL)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors.[?(@.property == 'legendType')]", hasSize(0)));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Fail if email value is wrong")
  void failIfEmailValueIsWrong() throws Exception {
    postEntityWithEmailValue(INVALID_EMAIL)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors.[?(@.property == '" + PROPERTY_WITH_EMAIL + "')].invalidValue", hasItem(INVALID_EMAIL)));
  }

  private ResultActions postEntityWithEmailValue(String validEmail) throws Exception {
    JSONObject entity = new JSONObject()
      .put(PROPERTY_WITH_EMAIL, validEmail);
    return mvc.perform(post(ENTITY_WITH_EMAIL_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(entity.toString())
    );
  }

}
