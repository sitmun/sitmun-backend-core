package org.sitmun.domain.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.domain.CodeListsConstants.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValueRepository;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("DatabaseConnection Repository Data REST test")
class DatabaseConnectionRepositoryDataRestTest {

  @Autowired private DatabaseConnectionRepository repository;

  @Autowired private MockMvc mvc;

  @Autowired private CodeListValueRepository codeListValueRepository;

  @BeforeEach
  void setUp() {
    // Increased timeout to 30 seconds to accommodate slower database initialization
    // in Oracle/PostgreSQL compared to H2, especially after context reloads
    // triggered by @DirtiesContext annotation
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .pollInterval(500, TimeUnit.MILLISECONDS)
        .until(
            () ->
                codeListValueRepository.existsByCodeListNameAndValue(
                    DATABASE_CONNECTION_DRIVER, "org.h2.Driver"));
  }

  @Test
  @DisplayName("GET: List DatabaseConnection")
  @WithMockUser(roles = "ADMIN")
  void tasksLinksExist() throws Exception {
    mvc.perform(get(CONNECTIONS_URI))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.connections.*", hasSize(1)))
        .andExpect(jsonPath("$._embedded.connections[*]._links.tasks", hasSize(1)))
        .andExpect(jsonPath("$._embedded.connections[*]._links.cartographies", hasSize(1)));
  }

  @Test
  @DisplayName("POST: Create a DatabaseConnection and then update the password")
  @WithMockUser(roles = "ADMIN")
  void updateUserPassword() throws Exception {
    String uri =
        mvc.perform(
                post(CONNECTIONS_URI)
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {
                          "driver" : "org.h2.Driver",
                          "url" : "jdbc:h2:mem:testdb",
                          "name" : "sa",
                          "password" : "password"
                        }"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.passwordSet").value(true))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(uri).isNotNull();

    String[] parts = uri.split("/");
    Integer id = Integer.parseInt(parts[parts.length - 1]);
    Optional<DatabaseConnection> databaseConnection = repository.findById(id);
    assertTrue(databaseConnection.isPresent());
    final String[] oldPassword = new String[1];
    oldPassword[0] = databaseConnection.get().getPassword();

    String content =
        """
        {
        "driver" : "org.h2.Driver",
        "url" : "jdbc:h2:mem:testdb",
        "name" : "sa",
        "password" : "new-password"
        }""";

    mvc.perform(put(uri).contentType(APPLICATION_JSON).content(content)).andExpect(status().isOk());

    databaseConnection = repository.findById(id);
    assertTrue(databaseConnection.isPresent());
    assertNotEquals(oldPassword[0], databaseConnection.get().getPassword());

    mvc.perform(delete(uri).contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Create a DatabaseConnection and then keep the password")
  @WithMockUser(roles = "ADMIN")
  void keepUserPassword() throws Exception {
    String uri =
        mvc.perform(
                post(CONNECTIONS_URI)
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {
                          "driver" : "org.h2.Driver",
                          "url" : "jdbc:h2:mem:testdb",
                          "name" : "sa",
                          "password" : "password"
                       }"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.passwordSet").value(true))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(uri).isNotNull();

    Integer id = TestUtils.extractId(uri);
    Optional<DatabaseConnection> databaseConnection = repository.findById(id);
    assertTrue(databaseConnection.isPresent());
    final String[] oldPassword = new String[1];
    oldPassword[0] = databaseConnection.get().getPassword();

    String content =
        """
        {
        "driver" : "org.h2.Driver",
        "url" : "jdbc:h2:mem:testdb",
        "name" : "sa"
        }
        """;

    mvc.perform(put(uri).contentType(APPLICATION_JSON).content(content)).andExpect(status().isOk());

    databaseConnection = repository.findById(id);
    assertTrue(databaseConnection.isPresent());
    assertEquals(oldPassword[0], databaseConnection.get().getPassword());

    mvc.perform(delete(uri).contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Create a DatabaseConnection and then clear the password")
  @WithMockUser(roles = "ADMIN")
  void clearUserPassword() throws Exception {
    String uri =
        mvc.perform(
                post(CONNECTIONS_URI)
                    .contentType(APPLICATION_JSON)
                    .content(
                        """
                        {
                          "driver" : "org.h2.Driver",
                          "url" : "jdbc:h2:mem:testdb",
                          "name" : "sa",
                          "password" : "password"
                         }"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.passwordSet").value(true))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    assertThat(uri).isNotNull();

    String[] parts = uri.split("/");
    Integer id = Integer.parseInt(parts[parts.length - 1]);

    String content =
        """
        {
          "driver" : "org.h2.Driver",
          "url" : "jdbc:h2:mem:testdb",
          "name" : "sa",
          "password" : ""
        }
      """;

    mvc.perform(put(uri).contentType(APPLICATION_JSON).content(content)).andExpect(status().isOk());

    Optional<DatabaseConnection> databaseConnection = repository.findById(id);
    assertTrue(databaseConnection.isPresent());
    assertNull(databaseConnection.get().getPassword());

    mvc.perform(delete(uri).contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }
}
