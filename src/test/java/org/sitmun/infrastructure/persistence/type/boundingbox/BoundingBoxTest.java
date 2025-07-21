package org.sitmun.infrastructure.persistence.type.boundingbox;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Bounding Box Utility Test")
class BoundingBoxTest extends BaseTest {

  @Test
  @DisplayName("Test bounding box creation")
  @WithMockUser(roles = "ADMIN")
  void testBoundingBoxCreation() throws Exception {
    Envelope envelope =
        Envelope.builder().minX(430492.0).minY(4611482.0).maxX(437423.0).maxY(4618759.0).build();
    MvcResult result = postWithCustomEnvelope(envelope).andExpect(status().isCreated()).andReturn();
    String location = result.getResponse().getHeader("Location");
    assertNotNull(location);
    mvc.perform(delete(location));
  }

  @Test
  @DisplayName("Test bounding box equality")
  @WithMockUser(roles = "ADMIN")
  void testBoundingBoxEquality() throws Exception {
    Envelope envelope =
        Envelope.builder().minX(430492.0).minY(4611482.0).maxX(null).maxY(-4618759.0).build();
    postWithCustomEnvelope(envelope)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors", hasSize(2)));
  }

  private ResultActions postWithCustomEnvelope(Envelope envelope) throws Exception {
    Territory territory =
        Territory.builder()
            .name("Fake Territory")
            .code("0000")
            .blocked(false)
            .extent(envelope)
            .build();
    String asString = new ObjectMapper().writeValueAsString(territory);
    return mvc.perform(
        post(URIConstants.TERRITORIES_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asString));
  }
}
