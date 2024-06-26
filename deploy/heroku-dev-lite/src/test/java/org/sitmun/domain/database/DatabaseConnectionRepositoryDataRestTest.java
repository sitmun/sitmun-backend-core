package org.sitmun.domain.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("DatabaseConnection Repository Data REST test")
class DatabaseConnectionRepositoryDataRestTest {

  @Autowired
  private DatabaseConnectionRepository repository;

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("GET: List DatabaseConnection")
  void tasksLinksExist() throws Exception {
    mvc.perform(get(URIConstants.CONNECTIONS_URI)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.connections.*", hasSize(1)))
      .andExpect(jsonPath("$._embedded.connections[*]._links.tasks", hasSize(1)))
      .andExpect(jsonPath("$._embedded.connections[*]._links.cartographies", hasSize(1)));
  }


  @Test
  @DisplayName("POST: Create a DatabaseConnection and then update the password")
  @WithMockUser(roles = "ADMIN")
  void updateUserPassword() throws Exception {
    String uri = mvc.perform(post(URIConstants.CONNECTIONS_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }")
        .with(user(Fixtures.admin())))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.passwordSet").value(true))
      .andReturn().getResponse().getHeader("Location");

    assertThat(uri).isNotNull();

    String[] parts = uri.split("/");
    Integer id = Integer.parseInt(parts[parts.length - 1]);
      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
    final String[] oldPassword = new String[1];
      oldPassword[0] = databaseConnection.get().getPassword();

    String content = "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"new-password\" }";

    mvc.perform(put(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

    databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      assertNotEquals(oldPassword[0], databaseConnection.get().getPassword());

    mvc.perform(delete(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Create a DatabaseConnection and then keep the password")
  @WithMockUser(roles = "ADMIN")
  void keepUserPassword() throws Exception {
    String uri = mvc.perform(post(URIConstants.CONNECTIONS_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }")
        .with(user(Fixtures.admin())))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.passwordSet").value(true))
      .andReturn().getResponse().getHeader("Location");

    assertThat(uri).isNotNull();

    String[] parts = uri.split("/");
    Integer id = Integer.parseInt(parts[parts.length - 1]);
      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
    final String[] oldPassword = new String[1];
      oldPassword[0] = databaseConnection.get().getPassword();

    String content = "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\"}";

    mvc.perform(put(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

    databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      assertEquals(oldPassword[0], databaseConnection.get().getPassword());

    mvc.perform(delete(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Create a DatabaseConnection and then clear the password")
  void clearUserPassword() throws Exception {
    String uri = mvc.perform(post(URIConstants.CONNECTIONS_URI)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }")
        .with(user(Fixtures.admin())))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.passwordSet").value(true))
      .andReturn().getResponse().getHeader("Location");

    assertThat(uri).isNotNull();

    String[] parts = uri.split("/");
    Integer id = Integer.parseInt(parts[parts.length - 1]);

    String content = "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"\"}";

    mvc.perform(put(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      assertNull(databaseConnection.get().getPassword());

    mvc.perform(delete(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

}
