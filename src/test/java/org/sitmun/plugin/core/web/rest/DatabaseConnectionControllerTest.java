package org.sitmun.plugin.core.web.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.Application;
import org.sitmun.plugin.core.domain.DatabaseConnection;
import org.sitmun.plugin.core.repository.DatabaseConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class DatabaseConnectionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DatabaseConnectionRepository repository;

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void failIfDatabaseConnectionDriverNotFound() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder().setDriver("org.h2.DriverX").build()));
    mockMvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("Driver not found"))
      .andExpect(jsonPath("$.cause").value("org.h2.DriverX"));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void failIfDatabaseConnectionException() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .setDriver("org.h2.Driver")
      .setUrl("jdb:h2:mem:testdb")
      .setName("sa")
      .setPassword("password")
      .build()));
    mockMvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.error").value("SQL exception"))
      .andExpect(jsonPath("$.cause").value("No suitable driver found for jdb:h2:mem:testdb"));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void databaseConnectionSuccess() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.of(DatabaseConnection.builder()
      .setDriver("org.h2.Driver")
      .setUrl("jdbc:h2:mem:testdb")
      .setName("sa")
      .setPassword("password")
      .build()));
    mockMvc.perform(get("/api/connections/0/test"))
      .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void failIfdatabaseConnectionNotFound() throws Exception {
    when(repository.findById(0)).thenReturn(Optional.empty());
    mockMvc.perform(get("/api/connections/0/test"))
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
}