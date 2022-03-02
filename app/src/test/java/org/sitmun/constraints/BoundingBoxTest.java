package org.sitmun.constraints;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.types.envelope.Envelope;
import org.sitmun.domain.Territory;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BoundingBox validation test")
public class BoundingBoxTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("Pass if meets conditions of BoundingBox")
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void passIfBoundingBoxIsValid() throws Exception {
    Envelope envelope = Envelope.builder()
      .minX(430492.0)
      .minY(4611482.0)
      .maxX(437423.0)
      .maxY(4618759.0).build();
    cleanup(postWithCustomEnvelope(envelope)
      .andExpect(status().isCreated()));
  }

  @Test
  @DisplayName("Fails if null values or max < min")
  public void failIfEmailValueIsWrong() throws Exception {
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
      .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin()))
    );
  }

  private void cleanup(ResultActions result) throws Exception {
    String location = result.andReturn().getResponse().getHeader("Location");
    assertNotNull(location);
    mvc.perform(delete(location).with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())));
  }

}
