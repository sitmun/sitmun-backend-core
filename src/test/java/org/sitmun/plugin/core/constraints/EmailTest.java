package org.sitmun.plugin.core.constraints;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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

public class EmailTest {

  private static final String VALID_EMAIL = "fake@example.com";
  private static final String INVALID_EMAIL = "fake @ example.com";
  private static final String ENTITY_WITH_EMAIL_URI = "http://localhost/api/territories";
  private static final String PROPERTY_WITH_EMAIL = "territorialAuthorityEmail";

  @Autowired
  private MockMvc mvc;

  @Test
  public void passIfEmailValueIsValid() throws Exception {
    postEntityWithEmailValue(VALID_EMAIL)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$." + PROPERTY_WITH_EMAIL, equalTo(VALID_EMAIL)));
  }

  @Test
  public void failIfEmailValueIsWrong() throws Exception {
    postEntityWithEmailValue(INVALID_EMAIL)
      .andExpect(status().is4xxClientError())
      .andExpect(jsonPath("$.errors[0].property", equalTo(PROPERTY_WITH_EMAIL)))
      .andExpect(jsonPath("$.errors[0].invalidValue", equalTo(INVALID_EMAIL)));
  }

  private ResultActions postEntityWithEmailValue(String validEmail) throws Exception {
    JSONObject entity = new JSONObject()
      .put("name", "Fake Territory")
      .put("code", "0000")
      .put("blocked", false);
    return mvc.perform(post(ENTITY_WITH_EMAIL_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(entity.put(PROPERTY_WITH_EMAIL, validEmail).toString())
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    );
  }

}
