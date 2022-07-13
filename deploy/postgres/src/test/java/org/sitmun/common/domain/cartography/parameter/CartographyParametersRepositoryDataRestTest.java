package org.sitmun.common.domain.cartography.parameter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.test.Fixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

class CartographyParametersRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void newCartographyParametersCanBePosted() throws Exception {
    String content = "{\"value\":\"test \",\"name\":\"test\",\"format\":\"I\",\"order\":null,\"type\":\"INFO\",\"status\":\"Pending creation\", \"cartography\":\"http://localhost/api/cartographies/0\"}";

    String location = mvc.perform(
        post("/api/cartography-parameters")
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn().getResponse().getHeader("Location");

    Assertions.assertNotNull(location);
    String id = new UriTemplate("http://localhost/api/cartography-parameters/{id}").match(location).get("id");

    mvc.perform(get("/api/cartography-parameters/{id}/cartography", id)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(0));

    mvc.perform(delete("/api/cartography-parameters/{id}", id)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  void newCartographyParameterRequiresCartographyLink() throws Exception {
    String content = "{\"value\":\"test \",\"name\":\"test\",\"format\":\"I\",\"order\":null,\"type\":\"INFO\",\"status\":\"Pending creation\"}";

    mvc.perform(
        post("/api/cartography-parameters?lang=EN")
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].property").value("cartography"))
      .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
  }

}
