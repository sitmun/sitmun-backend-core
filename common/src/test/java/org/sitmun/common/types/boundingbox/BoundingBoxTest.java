package org.sitmun.common.types.boundingbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.types.envelope.Envelope;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("BoundingBox validation test")
class BoundingBoxTest extends BaseTest {

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
