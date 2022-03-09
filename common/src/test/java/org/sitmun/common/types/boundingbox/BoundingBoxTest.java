package org.sitmun.common.types.boundingbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.types.envelope.Envelope;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("BoundingBox validation test")
public class BoundingBoxTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @BeforeEach
  public void setup() {
    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @Test
  @DisplayName("Pass if meets conditions of BoundingBox")
  @WithMockUser(roles = {"ADMIN"})
  public void passIfBoundingBoxIsValid() throws Exception {
    Envelope envelope = Envelope.builder()
      .minX(430492.0)
      .minY(4611482.0)
      .maxX(437423.0)
      .maxY(4618759.0).build();
    postWithCustomEnvelope(envelope)
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Fails if null values or max < min")
  @WithMockUser(roles = {"ADMIN"})
  public void failIfNullValuesOrMaxLessThanMin() throws Exception {
    Envelope envelope = Envelope.builder()
      .minX(430492.0)
      .minY(4611482.0)
      .maxX(null)
      .maxY(-4618759.0).build();
    postWithCustomEnvelope(envelope)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors", hasSize(2)));
  }

  private ResultActions postWithCustomEnvelope(Envelope envelope) throws Exception {
    Territory territory = Territory.builder().name("Fake Territory").code("0000").blocked(false).extent(envelope).build();
    String asString = new ObjectMapper().writeValueAsString(territory);
    return mvc.perform(post(URIConstants.TERRITORIES_URI)
      .contentType(MediaType.APPLICATION_JSON)
      .content(asString)
    );
  }
}
