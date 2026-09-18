package org.sitmun.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.SitmunClientTypes;
import org.sitmun.authentication.dto.AuthenticationResponse;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Authentication Controller basic tests")
class AuthenticationControllerTest {

  private static final int COOKIE_MAX_AGE_SECONDS = 900;

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JsonWebTokenService jsonWebTokenService;
  @Autowired private UserDetailsService userDetailsService;
  @Autowired private UserRepository userRepository;
  @Autowired private UserPositionRepository userPositionRepository;
  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private final List<User> persistedLoginUsers = new ArrayList<>();

  @AfterEach
  void deletePersistedLoginUsers() {
    for (User user : persistedLoginUsers) {
      if (user.getId() != null) {
        userPositionRepository.deleteAll(userPositionRepository.findByUser(user));
        userRepository.deleteById(user.getId());
      }
    }
    persistedLoginUsers.clear();
  }

  @Test
  @DisplayName("POST /authenticate: viewer login issues viewer_access_token cookie")
  void viewerLoginIssuesViewerCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME))
        .andExpect(
            cookie()
                .maxAge(
                    AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME,
                    COOKIE_MAX_AGE_SECONDS))
        .andExpect(cookie().doesNotExist(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME));
  }

  @Test
  @DisplayName("POST /authenticate: viewer login expires legacy access_token cookie")
  void viewerLoginExpiresLegacyCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /authenticate/admin: admin login issues admin_access_token cookie")
  void adminLoginIssuesAdminCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME))
        .andExpect(cookie().doesNotExist(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME));
  }

  @Test
  @DisplayName("POST /authenticate/admin: admin login expires legacy access_token cookie")
  void adminLoginExpiresLegacyCookie() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /authenticate: wrong credentials returns 401")
  void loginFailure() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("other");

    mvc.perform(
            post("/api/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /proxy: Authenticated user must get short-lived token in response")
  void proxyAuthenticationSuccess() throws Exception {
    MvcResult result =
        mvc.perform(post("/api/authenticate/proxy").secure(true))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    AuthenticationResponse response = objectMapper.readValue(content, AuthenticationResponse.class);
    assertThat(response.getProxyToken()).isNotNull().isNotEmpty();
  }

  @Test
  @WithMockUser(
      username = "normal-user",
      roles = {"USER"})
  @DisplayName("POST /proxy: Standard user must get short-lived token in response")
  void proxyAuthenticationSuccessForStandardUser() throws Exception {
    MvcResult result =
        mvc.perform(post("/api/authenticate/proxy").secure(true))
            .andExpect(status().isOk())
            .andReturn();

    String content = result.getResponse().getContentAsString();
    AuthenticationResponse response = objectMapper.readValue(content, AuthenticationResponse.class);
    assertThat(response.getProxyToken()).isNotNull().isNotEmpty();
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /logout: without selector header clears viewer_access_token")
  void logoutClearsViewerCookie() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /logout: with X-SITMUN-Client: admin clears admin_access_token")
  void logoutWithAdminHeaderClearsAdminCookie() throws Exception {
    mvc.perform(
            post("/api/authenticate/logout")
                .secure(true)
                .header(SitmunClientTypes.HEADER_NAME, "admin"))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN", "USER"})
  @DisplayName("POST /logout: always expires legacy access_token cookie")
  void logoutExpiresLegacyCookie() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @WithMockUser(
      username = "normal-user",
      roles = {"USER"})
  @DisplayName("POST /logout: Standard user must logout successfully")
  void logoutSuccessForStandardUser() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /proxy: Anonymous user must be denied")
  void proxyAuthenticationDeniedForAnonymous() throws Exception {
    mvc.perform(post("/api/authenticate/proxy").secure(true)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /logout: Anonymous user can clear stale session cookie")
  void logoutAllowedForAnonymous() throws Exception {
    mvc.perform(post("/api/authenticate/logout").secure(true))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /refresh: copies auth_time and issues a new viewer cookie")
  void refreshCopiesAuthTime() throws Exception {
    Cookie session = viewerLoginCookie("admin", "admin");
    Long authTime = jsonWebTokenService.getAuthTimeMillis(session.getValue());
    assertThat(authTime).isNotNull();

    MvcResult refreshed =
        mvc.perform(post("/api/authenticate/refresh").secure(true).cookie(session))
            .andExpect(status().isOk())
            .andExpect(cookie().exists(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME))
            .andExpect(
                cookie()
                    .maxAge(
                        AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME,
                        COOKIE_MAX_AGE_SECONDS))
            .andReturn();

    Cookie next =
        refreshed.getResponse().getCookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME);
    assertThat(next).isNotNull();
    assertThat(jsonWebTokenService.getAuthTimeMillis(next.getValue())).isEqualTo(authTime);
  }

  @Test
  @DisplayName("POST /refresh: 401 when auth_time is older than the session cap")
  void refreshUnauthorizedWhenAuthTimeExceedsCap() throws Exception {
    UserDetails admin = userDetailsService.loadUserByUsername("admin");
    Date lastPasswordChange =
        userRepository.findByUsername("admin").map(User::getLastPasswordChange).orElse(null);
    Date issued = new Date();
    Date authTime = new Date(issued.getTime() - 9 * 3_600_000L);
    String token =
        jsonWebTokenService.generateToken(
            admin.getUsername(), issued, lastPasswordChange, 900_000, authTime);
    Cookie session = new Cookie(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, token);

    mvc.perform(post("/api/authenticate/refresh").secure(true).cookie(session))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME, 0));
  }

  @Test
  @DisplayName("POST /refresh: admin cookie succeeds with zero positions")
  void adminRefreshSucceedsWithZeroPositions() throws Exception {
    User operator = persistLoginUser("ra", true);
    Cookie session = adminLoginCookie(operator.getUsername(), "secret");

    mvc.perform(
            post("/api/authenticate/refresh")
                .secure(true)
                .cookie(session)
                .header(SitmunClientTypes.HEADER_NAME, "admin"))
        .andExpect(status().isOk())
        .andExpect(cookie().exists(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME));
  }

  @Test
  @DisplayName("POST /refresh: viewer cookie 401 when the user has no live positions")
  void viewerRefreshUnauthorizedWhenNoLivePositions() throws Exception {
    User user = persistLoginUser("rk", false);
    Cookie session = viewerLoginCookie(user.getUsername(), "secret");

    mvc.perform(post("/api/authenticate/refresh").secure(true).cookie(session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"MOBILE_EDITION"})
  @DisplayName("POST /refresh: edition principal does not mint a viewer cookie")
  void mobileEditionRefreshDoesNotMintViewerCookie() throws Exception {
    mvc.perform(post("/api/authenticate/refresh").secure(true))
        .andExpect(status().isForbidden())
        .andExpect(cookie().doesNotExist(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME));
  }

  @Test
  @DisplayName("POST /proxy: still 200 when every UserPosition is expired")
  void proxySucceedsWhenEveryPositionExpired() throws Exception {
    User user = persistLoginUser("rp", false);
    Territory territory = territoryRepository.findAll().iterator().next();
    Date expired = new Date(System.currentTimeMillis() - 86_400_000L);
    UserPosition position =
        userPositionRepository.save(
            UserPosition.builder()
                .user(user)
                .territory(territory)
                .name("cargo")
                .organization("org")
                .createdDate(expired)
                .expirationDate(expired)
                .build());
    position.setCreatedDate(expired);
    position.setExpirationDate(expired);
    userPositionRepository.save(position);

    Cookie session = viewerLoginCookie(user.getUsername(), "secret");
    mvc.perform(post("/api/authenticate/proxy").secure(true).cookie(session))
        .andExpect(status().isOk());
  }

  private Cookie viewerLoginCookie(String username, String password) throws Exception {
    return loginCookie(
        "/api/authenticate",
        username,
        password,
        AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME);
  }

  private Cookie adminLoginCookie(String username, String password) throws Exception {
    return loginCookie(
        "/api/authenticate/admin",
        username,
        password,
        AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME);
  }

  private Cookie loginCookie(String path, String username, String password, String cookieName)
      throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername(username);
    login.setPassword(password);
    MvcResult result =
        mvc.perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtils.asJsonString(login)))
            .andExpect(status().isOk())
            .andReturn();
    Cookie cookie = result.getResponse().getCookie(cookieName);
    assertThat(cookie).isNotNull();
    return cookie;
  }

  private User persistLoginUser(String prefix, boolean administrator) {
    String id = UUID.randomUUID().toString().substring(0, 8);
    User user = new User();
    user.setUsername(prefix + id);
    user.setPassword(passwordEncoder.encode("secret"));
    user.setAdministrator(administrator);
    user.setBlocked(false);
    user.setFirstName("Refresh");
    user.setLastName("Test");
    user.setEmail(id + "@ex.com");
    User saved = userRepository.save(user);
    persistedLoginUsers.add(saved);
    return saved;
  }
}
