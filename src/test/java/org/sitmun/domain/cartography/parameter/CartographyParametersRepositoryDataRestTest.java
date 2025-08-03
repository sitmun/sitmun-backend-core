package org.sitmun.domain.cartography.parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CartographyParameters Repository Data REST test")
class CartographyParametersRepositoryDataRestTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("POST: Create a CartographyParameter")
  @WithMockUser(roles = "ADMIN")
  void newCartographyParametersCanBePosted() throws Exception {
    String content =
        """
        {
        "value":"test",
        "name":"test",
        "format":"I",
        "order":null,
        "type":"INFO",
        "cartography":"http://localhost/api/cartographies/1"
        }""";

    String location =
        mvc.perform(post("/api/cartography-parameters").content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertNotNull(location);
    String id =
        new UriTemplate("http://localhost/api/cartography-parameters/{id}")
            .match(location)
            .get("id");

    mvc.perform(get("/api/cartography-parameters/{id}/cartography", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));

    mvc.perform(delete("/api/cartography-parameters/{id}", id)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: new cartography parameter requires a cartography link")
  @WithMockUser(roles = "ADMIN")
  void newCartographyParameterRequiresCartographyLink() throws Exception {
    String content =
        """
        {
         "value":"test",
         "name":"test",
         "format":"I",
         "order":null,
         "type":"INFO"
         }
        """;

    mvc.perform(post("/api/cartography-parameters?lang=EN").content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].property").value("cartography"))
        .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
  }
}
