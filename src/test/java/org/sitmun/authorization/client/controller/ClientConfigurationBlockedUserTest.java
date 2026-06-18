package org.sitmun.authorization.client.controller;

import static org.mockito.Mockito.when;
import static org.sitmun.infrastructure.security.core.SecurityConstants.PUBLIC_PRINCIPAL;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Client Configuration — blocked user access")
class ClientConfigurationBlockedUserTest {

  private static final String BLOCKED_USERNAME = "blocked";
  private static final int APP_ID = 1;
  private static final int TER_ID = 1;

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private JsonWebTokenService jsonWebTokenService;

  @MockitoBean private ApplicationRepository applicationRepository;

  @Test
  @DisplayName("denies blocked public principal on application list with 401")
  void deniesBlockedPublicOnApplicationList() throws Exception {
    blockPublicUser();

    mockMvc
        .perform(get(CONFIG_CLIENT_APPLICATION_URI).contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("denies blocked public principal on territory list with 401")
  void deniesBlockedPublicOnTerritoryList() throws Exception {
    blockPublicUser();

    mockMvc
        .perform(get(CONFIG_CLIENT_TERRITORY_URI).contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("denies blocked public principal on profile with 401")
  void deniesBlockedPublicOnProfile() throws Exception {
    blockPublicUser();
    when(applicationRepository.findById(APP_ID))
        .thenReturn(Optional.of(Application.builder().id(APP_ID).appPrivate(false).build()));

    mockMvc
        .perform(get(CONFIG_CLIENT_PROFILE_URI, APP_ID, TER_ID).contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("denies public principal on private app profile with 401")
  void deniesPublicOnPrivateAppProfile() throws Exception {
    when(applicationRepository.findById(APP_ID))
        .thenReturn(Optional.of(Application.builder().id(APP_ID).appPrivate(true).build()));

    mockMvc
        .perform(get(CONFIG_CLIENT_PROFILE_URI, APP_ID, TER_ID).contentType(APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("denies blocked authenticated user on application list with 403")
  @WithMockUser(username = BLOCKED_USERNAME, roles = "USER")
  void deniesBlockedAuthenticatedOnApplicationList() throws Exception {
    mockMvc
        .perform(get(CONFIG_CLIENT_APPLICATION_URI).contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("denies blocked authenticated user on territory position with 403")
  @WithMockUser(username = BLOCKED_USERNAME, roles = "USER")
  void deniesBlockedAuthenticatedOnTerritoryPosition() throws Exception {
    mockMvc
        .perform(
            post(CONFIG_CLIENT_URI + "/territory/position")
                .contentType(APPLICATION_JSON)
                .content("{\"id\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("denies blocked authenticated user on application territories with 403")
  @WithMockUser(username = BLOCKED_USERNAME, roles = "USER")
  void deniesBlockedAuthenticatedOnApplicationTerritories() throws Exception {
    mockMvc
        .perform(
            get(CONFIG_CLIENT_APPLICATION_TERRITORIES_URI, APP_ID).contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("denies blocked authenticated user on profile with 403")
  @WithMockUser(username = BLOCKED_USERNAME, roles = "USER")
  void deniesBlockedAuthenticatedOnProfile() throws Exception {
    mockMvc
        .perform(get(CONFIG_CLIENT_PROFILE_URI, APP_ID, TER_ID).contentType(APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("rejects JWT cookie for blocked authenticated user with 401 and clears the cookie")
  void rejectsJwtCookieForBlockedAuthenticatedUser() throws Exception {
    String jwt = jsonWebTokenService.generateToken(BLOCKED_USERNAME, new Date());

    mockMvc
        .perform(
            get(CONFIG_CLIENT_APPLICATION_URI)
                .contentType(APPLICATION_JSON)
                .cookie(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, jwt)))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("rejects JWT cookie for blocked public user with 401 and clears the cookie")
  void rejectsJwtCookieForBlockedPublicUser() throws Exception {
    blockPublicUser();
    String jwt = jsonWebTokenService.generateToken(PUBLIC_PRINCIPAL, new Date());

    mockMvc
        .perform(
            get(CONFIG_CLIENT_APPLICATION_URI)
                .contentType(APPLICATION_JSON)
                .cookie(new Cookie(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, jwt)))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  private void blockPublicUser() {
    User publicUser =
        userRepository
            .findByUsername(PUBLIC_PRINCIPAL)
            .orElseThrow(() -> new IllegalStateException("built-in public user missing"));
    publicUser.setBlocked(true);
    userRepository.save(publicUser);
  }
}
