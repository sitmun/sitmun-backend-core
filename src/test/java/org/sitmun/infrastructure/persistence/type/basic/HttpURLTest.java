package org.sitmun.infrastructure.persistence.type.basic;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Http validation test")
class HttpURLTest extends BaseTest {

  private static final String VALID_HTTP_URL = "http://example.com/somefile";
  private static final String VALID_HTTPS_URL = "https://example.com/somefile";
  private static final String INVALID_URL = "ftp://example.com/somefile";
  private static final String ENTITY_WITH_URL_URI = "http://localhost/api/territories";
  private static final String PROPERTY_WITH_URL = "territorialAuthorityLogo";

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Pass if URL value is http")
  void passIfURLValueIsHttp() throws Exception {
    postEntityWithUrlValue(VALID_HTTP_URL)
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors.[?(@.property == '" + PROPERTY_WITH_URL + "')]", hasSize(0)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Pass if URL value is https")
  void passIfURLValueIsHttps() throws Exception {
    postEntityWithUrlValue(VALID_HTTPS_URL)
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.errors.[?(@.property == '" + PROPERTY_WITH_URL + "')]", hasSize(0)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Fail if URL value is wrong")
  void failIfURLValueIsWrong() throws Exception {
    postEntityWithUrlValue(INVALID_URL)
        .andDo(print())
        .andExpect(status().is4xxClientError())
        .andExpect(
            jsonPath(
                "$.errors.[?(@.property == '" + PROPERTY_WITH_URL + "')].invalidValue",
                hasItem(INVALID_URL)));
  }

  private ResultActions postEntityWithUrlValue(String value) throws Exception {
    JSONObject entity = new JSONObject().put(PROPERTY_WITH_URL, value);
    return mvc.perform(
        post(ENTITY_WITH_URL_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(entity.toString()));
  }
}
