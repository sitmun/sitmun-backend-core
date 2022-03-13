package org.sitmun.feature.database.tester;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.common.domain.database.DatabaseConnection;
import org.sitmun.common.domain.database.DatabaseConnectionRepository;
import org.sitmun.test.BaseTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Database connection controller test")
public class DatabaseConnectionControllerTest extends BaseTest {

  @MockBean
  private DatabaseConnectionRepository repository;

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Fail 500 when driver is not found in test")
  public void failIfDatabaseConnectionDriverNotFound() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder().driver("org.h2.DriverX").build()));
    mvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("Internal Server Error"))
      .andExpect(jsonPath("$.message").value("java.lang.ClassNotFoundException: org.h2.DriverX"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Fail 500 when test fails")
  public void failIfDatabaseConnectionException() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .driver("org.h2.Driver")
      .url("jdb:h2:mem:testdb")
      .name("sa")
      .password("password")
      .build()));
    mvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("Internal Server Error"))
      .andExpect(jsonPath("$.message").value("java.sql.SQLException: No suitable driver found for jdb:h2:mem:testdb"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Return 200 when test succeeds")
  public void databaseConnectionSuccess() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .driver("org.h2.Driver")
      .url("jdbc:h2:mem:testdb")
      .name("sa")
      .password("password")
      .build()));
    mvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Return 404 when the connection does not exist")
  public void failIfdatabaseConnectionNotFound() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.empty());
    mvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Return 401 is the test request has no credentials")
  public void failIfNoCredentials() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .driver("org.h2.Driver")
      .url("jdbc:h2:mem:testdb")
      .name("sa")
      .password("password")
      .build()));
    mvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Cant create entry if the database connection driver is not found")
  public void failPostTestIfDatabaseConnectionDriverNotFound() throws Exception {
    mvc.perform(post("/api/connections/test")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"driver\" : \"org.h2.DriverX\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("Internal Server Error"))
      .andExpect(jsonPath("$.message").value("java.lang.ClassNotFoundException: org.h2.DriverX"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Cant create entry if the database connection fails")
  public void failPostTestIfDatabaseConnectionException() throws Exception {
    mvc.perform(post("/api/connections/test")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdb:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("Internal Server Error"))
      .andExpect(jsonPath("$.message").value("java.sql.SQLException: No suitable driver found for jdb:h2:mem:testdb"));
  }

  @Test
  @WithMockUser(roles = {"ADMIN"})
  @DisplayName("Return 200 when the test succeed")
  public void databaseConnectionPostTestSuccess() throws Exception {
    mvc.perform(post("/api/connections/test")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }"))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Return 401 is the test creation has no credentials")
  public void failPostTestIfNoCredentials() throws Exception {
    mvc.perform(post("/api/connections/test")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }"))
      .andExpect(status().isUnauthorized());
  }
}