package org.sitmun.infrastructure.persistence.type.basic;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Email validation test")
class EmailTest extends BaseTest {

  private static final String INVALID_EMAIL = "fake @ example.com";
  private static final String ENTITY_WITH_EMAIL_URI = URIConstants.TERRITORIES_URI;
  private static final String PROPERTY_WITH_EMAIL = "territorialAuthorityEmail";

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("A bad email is rejected on territorialAuthorityEmail")
  void failIfEmailValueIsWrong() throws Exception {
    postEntityWithEmailValue(INVALID_EMAIL)
        .andExpect(status().is4xxClientError())
        .andExpect(
            jsonPath(
                "$.errors.[?(@.field == '" + PROPERTY_WITH_EMAIL + "')].rejectedValue",
                hasItem(INVALID_EMAIL)));
  }

  private ResultActions postEntityWithEmailValue(String validEmail) throws Exception {
    JSONObject entity = new JSONObject().put(PROPERTY_WITH_EMAIL, validEmail);
    return mvc.perform(
        post(ENTITY_WITH_EMAIL_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(entity.toString()));
  }
}
