package org.sitmun.repository.rest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.sitmun.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class CartographyParametersRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void newCartographyParametersCanBePosted() throws Exception {
    String content = "{\"value\":\"test \",\"name\":\"test\",\"format\":\"I\",\"order\":null,\"type\":\"INFO\",\"status\":\"Pending creation\", \"cartography\":\"http://localhost/api/cartographies/0\"}";

    String location = mvc.perform(
      post("/api/cartography-parameters")
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    assertNotNull(location);
    String id = new UriTemplate("http://localhost/api/cartography-parameters/{id}").match(location).get("id");

    mvc.perform(get("/api/cartography-parameters/{id}/cartography", id)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(0));

    mvc.perform(delete("/api/cartography-parameters/{id}", id)
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isNoContent());
  }

  @Test
  public void newCartographyParameterRequiresCartographyLink() throws Exception {
    String content = "{\"value\":\"test \",\"name\":\"test\",\"format\":\"I\",\"order\":null,\"type\":\"INFO\",\"status\":\"Pending creation\"}";

    mvc.perform(
      post("/api/cartography-parameters")
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("cartography"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
  }

}
