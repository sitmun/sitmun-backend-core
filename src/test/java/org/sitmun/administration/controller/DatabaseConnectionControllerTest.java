package org.sitmun.administration.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.database.DatabaseConnectionRepository;
import org.sitmun.test.BaseTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Database connection controller test")
class DatabaseConnectionControllerTest extends BaseTest {

  @MockitoBean private DatabaseConnectionRepository repository;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET returns 400 when the driver class is missing")
  void failIfDatabaseConnectionDriverNotFound() throws Exception {
    when(repository.findById(0))
        .thenReturn(Optional.of(DatabaseConnection.builder().driver("org.h2.DriverX").build()));
    mvc.perform(get("/api/connections/0/test"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("java.lang.ClassNotFoundException: org.h2.DriverX"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET returns valid true")
  void getReturnsValidTrue() throws Exception {
    when(repository.findById(0))
        .thenReturn(
            Optional.of(
                DatabaseConnection.builder()
                    .driver("org.h2.Driver")
                    .url("jdbc:h2:mem:testdb")
                    .user("sa")
                    .password(null)
                    .build()));
    mvc.perform(get("/api/connections/0/test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Return 404 when the connection does not exist")
  void failIfdatabaseConnectionNotFound() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.empty());
    mvc.perform(get("/api/connections/0/test")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET without credentials returns 401")
  void failIfNoCredentials() throws Exception {
    when(repository.findById(0))
        .thenReturn(
            Optional.of(
                DatabaseConnection.builder()
                    .driver("org.h2.Driver")
                    .url("jdbc:h2:mem:testdb")
                    .user("sa")
                    .password(null)
                    .build()));
    mvc.perform(get("/api/connections/0/test")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST returns 400 when the driver class is missing")
  void failPostTestIfDatabaseConnectionDriverNotFound() throws Exception {
    mvc.perform(
            post("/api/connections/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{ \"driver\" : \"org.h2.DriverX\", \"url\" : \"jdbc:h2:mem:testdb\", \"user\" : \"sa\", \"password\" : \"password\" }"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("java.lang.ClassNotFoundException: org.h2.DriverX"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST returns 400 when the JDBC URL is malformed")
  void failPostTestIfDatabaseConnectionException() throws Exception {
    mvc.perform(
            post("/api/connections/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb;MODE=NOT_A_MODE\", \"user\" : \"sa\", \"password\" : null }"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("Unknown mode \"NOT_A_MODE\"")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("POST returns valid true")
  void postReturnsValidTrue() throws Exception {
    mvc.perform(
            post("/api/connections/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"user\" : \"sa\", \"password\" : null }"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true));
  }

  @Test
  @DisplayName("POST without credentials returns 401")
  void failPostTestIfNoCredentials() throws Exception {
    mvc.perform(
            post("/api/connections/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"user\" : \"sa\", \"password\" : null }"))
        .andExpect(status().isUnauthorized());
  }
}
