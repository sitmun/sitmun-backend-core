package org.sitmun.plugin.core.web.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.Application;
import org.sitmun.plugin.core.domain.DatabaseConnection;
import org.sitmun.plugin.core.repository.DatabaseConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class DatabaseConnectionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DatabaseConnectionRepository repository;

  @Test
  public void failIfDatabaseConnectionDriverNotFound() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder().setDriver("org.h2.DriverX").build()));
    mockMvc.perform(get("/api/connections/0/test")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("Driver not found"))
      .andExpect(jsonPath("$.message").value("org.h2.DriverX"));
  }

  @Test
  public void failIfDatabaseConnectionException() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .setDriver("org.h2.Driver")
      .setUrl("jdb:h2:mem:testdb")
      .setName("sa")
      .setPassword("password")
      .build()));
    mockMvc.perform(get("/api/connections/0/test")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("SQL exception"))
      .andExpect(jsonPath("$.message").value("No suitable driver found for jdb:h2:mem:testdb"));
  }

  @Test
  public void databaseConnectionSuccess() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .setDriver("org.h2.Driver")
      .setUrl("jdbc:h2:mem:testdb")
      .setName("sa")
      .setPassword("password")
      .build()));
    mockMvc.perform(get("/api/connections/0/test")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk());
  }

  @Test
  public void failIfdatabaseConnectionNotFound() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.empty());
    mockMvc.perform(get("/api/connections/0/test")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isNotFound());
  }

  @Test
  public void failIfNoCredentials() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .setDriver("org.h2.Driver")
      .setUrl("jdbc:h2:mem:testdb")
      .setName("sa")
      .setPassword("password")
      .build()));
    mockMvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isUnauthorized());
  }

  @Test
  public void failPostTestIfDatabaseConnectionDriverNotFound() throws Exception {
    mockMvc.perform(post("/api/connections/test")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{ \"driver\" : \"org.h2.DriverX\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("Driver not found"))
      .andExpect(jsonPath("$.message").value("org.h2.DriverX"));
  }

  @Test
  public void failPostTestIfDatabaseConnectionException() throws Exception {
    mockMvc.perform(post("/api/connections/test")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdb:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("SQL exception"))
      .andExpect(jsonPath("$.message").value("No suitable driver found for jdb:h2:mem:testdb"));
  }

  @Test
  public void databaseConnectionPostTestSuccess() throws Exception {
    mockMvc.perform(post("/api/connections/test")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }")
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
      .andExpect(status().isOk());
  }

  @Test
  public void failPostTestIfNoCredentials() throws Exception {
    mockMvc.perform(post("/api/connections/test")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{ \"driver\" : \"org.h2.Driver\", \"url\" : \"jdbc:h2:mem:testdb\", \"name\" : \"sa\", \"password\" : \"password\" }"))
      .andExpect(status().isUnauthorized());
  }
}