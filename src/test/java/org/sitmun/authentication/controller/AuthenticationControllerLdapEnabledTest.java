package org.sitmun.authentication.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.test.AdditiveActiveProfiles;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AdditiveActiveProfiles(value = Profiles.LDAP)
@DisplayName("Authentication Controller LDAP enabled tests")
class AuthenticationControllerLdapEnabledTest {

  @Value("${sitmun.authentication.ldap.url}")
  private String url;

  @Value("${sitmun.authentication.ldap.base-dn}")
  private String baseDN;

  @Value("${test.ldap.ldif}")
  private String ldif;

  @Value("${test.ldap.schema}")
  private String schema;

  @Autowired private MockMvc mvc;

  private InMemoryDirectoryServer directoryServer;

  @BeforeEach
  void setupLdapServer() throws LDAPException {
    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDN);
    int port = URI.create(url).getPort();
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", port));
    directoryServer = new InMemoryDirectoryServer(config);
    directoryServer.applyChangesFromLDIF(schema);
    directoryServer.importFromLDIF(false, ldif);
    directoryServer.startListening();
  }

  @AfterEach
  void shutdownLdapServer() {
    directoryServer.shutDown(true);
  }

  @Test
  @DisplayName("POST: A user that fails in LDAP and exists in SITMUN, must pass.")
  void ldapFailureUserDetailsSuccessfulLogin() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id_token").exists());
  }

  @Test
  @DisplayName("POST: A user that exists in LDAP and exists in SITMUN, must pass.")
  void successfulLdapLogin() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("internal");
    login.setPassword("password12");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id_token").exists());
  }

  @Test
  @DisplayName("POST: A user that exists in LDAP and does not exist in SITMUN, must fail.")
  void successfulLdapLoginNoExistsSitmun() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("nositmunuser");
    login.setPassword("nositmunpassword");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isUnauthorized());
  }
}
