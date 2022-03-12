package org.sitmun.repository.rest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.database.DatabaseConnection;
import org.sitmun.common.domain.database.DatabaseConnectionRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class DatabaseConnectionRepositoryDataRestTest {

  @Autowired
  private DatabaseConnectionRepository repository;

  @Autowired
  private MockMvc mvc;

  @Test
  public void tasksLinksExist() throws Exception {
    mvc.perform(get(URIConstants.CONNECTIONS_URI)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.connections.*", hasSize(16)))
      .andExpect(jsonPath("$._embedded.connections[*]._links.tasks", hasSize(16)));
  }

  @Test
  public void cartographiesLinksExist() throws Exception {
    mvc.perform(get(URIConstants.CONNECTIONS_URI)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.connections.*", hasSize(16)))
      .andExpect(jsonPath("$._embedded.connections[*]._links.cartographies", hasSize(16)));
  }


  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void updateUserPassword() throws Exception {
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
    final String[] oldPassword = new String[1];
    withMockSitmunAdmin(() -> {
      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      oldPassword[0] = databaseConnection.get().getPassword();
    });

    String content = "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"new-password\" }";

    mvc.perform(put(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

    withMockSitmunAdmin(() -> {
      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      assertNotEquals(oldPassword[0], databaseConnection.get().getPassword());
    });

    mvc.perform(delete(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void keepUserPassword() throws Exception {
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
    final String[] oldPassword = new String[1];
    withMockSitmunAdmin(() -> {
      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      oldPassword[0] = databaseConnection.get().getPassword();
    });

    String content = "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\"}";

    mvc.perform(put(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isOk());

    withMockSitmunAdmin(() -> {
      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      assertEquals(oldPassword[0], databaseConnection.get().getPassword());
    });

    mvc.perform(delete(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  public void clearUserPassword() throws Exception {
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

    withMockSitmunAdmin(() -> {
      Optional<DatabaseConnection> databaseConnection = repository.findById(id);
      assertTrue(databaseConnection.isPresent());
      assertNull(databaseConnection.get().getPassword());
    });

    mvc.perform(delete(uri)
      .contentType(MediaType.APPLICATION_JSON)
      .with(user(Fixtures.admin()))
    ).andExpect(status().isNoContent());
  }

}
