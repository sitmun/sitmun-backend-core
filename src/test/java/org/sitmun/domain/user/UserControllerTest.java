package org.sitmun.domain.user;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.controller.AuthenticationController;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP contracts for {@code /api/account}. Handler rules live in {@link UserEventHandlerTest}.
 * Fixtures use a pre-encoded password (plain {@code save} does not fire
 * {@code @HandleBeforeCreate}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Controller Test")
class UserControllerTest {

  private static final String USER_PASSWORD = "user";
  private static final String USER_FIRSTNAME = "Admin";
  private static final String USER_LASTNAME = "Admin";
  private static final Boolean USER_BLOCKED = false;
  private static final Boolean USER_ADMINISTRATOR = false;
  private static final String ACCESS_TOKEN =
      AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME;
  @Autowired JsonWebTokenService tokenProvider;
  @Autowired private MockMvc mvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  private String username;
  private String validToken;
  private User user;
  private String expiredToken;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void init() {
    username = "acct-user-" + UUID.randomUUID().toString().substring(0, 8);
    Date expiredDate =
        Date.from(LocalDate.parse("1900-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant());
    expiredToken = tokenProvider.generateToken(username, expiredDate);
    validToken = tokenProvider.generateToken(username, new Date());

    user =
        User.builder()
            .administrator(USER_ADMINISTRATOR)
            .blocked(USER_BLOCKED)
            .firstName(USER_FIRSTNAME)
            .lastName(USER_LASTNAME)
            .password(passwordEncoder.encode(USER_PASSWORD))
            .username(username)
            .build();
    user = userRepository.save(user);
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    if (user != null && user.getId() != null) {
      userRepository.deleteById(user.getId());
    }
  }

  @Test
  @DisplayName("GET: Read user account with valid token")
  void readAccount() throws Exception {
    mvc.perform(get(URIConstants.ACCOUNT_URI).cookie(new Cookie(ACCESS_TOKEN, validToken)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.firstName", equalTo(USER_FIRSTNAME)))
        .andExpect(jsonPath("$.lastName", equalTo(USER_LASTNAME)));
  }

  @Test
  @DisplayName("GET: Read user account without token should fail")
  void readAccountWithoutToken() throws Exception {
    mvc.perform(get(URIConstants.ACCOUNT_URI))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type", equalTo(ProblemTypes.UNAUTHORIZED)))
        .andExpect(jsonPath("$.instance", equalTo("/api/account")));
  }

  @Test
  @DisplayName("GET: Read user account with expired token should fail")
  void readAccountWithExpiredToken() throws Exception {
    mvc.perform(get(URIConstants.ACCOUNT_URI).cookie(new Cookie(ACCESS_TOKEN, expiredToken)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST: Update account but keep the password")
  void updateAccountButKeepThePassword() throws Exception {
    String content =
        """
        {
        "username":"%s",
        "firstName":"NameChanged",
        "lastName":"NameChanged",
        "administrator": false,
        "blocked": false
        }
        """
            .formatted(username);

    mvc.perform(
            post(URIConstants.ACCOUNT_URI)
                .cookie(new Cookie(ACCESS_TOKEN, validToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
        .andExpect(status().isOk());

    mvc.perform(get(URIConstants.ACCOUNT_URI).cookie(new Cookie(ACCESS_TOKEN, validToken)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.firstName", equalTo("NameChanged")))
        .andExpect(jsonPath("$.lastName", equalTo("NameChanged")))
        .andExpect(jsonPath("$.passwordSet", equalTo(true)))
        .andExpect(jsonPath("$.password").doesNotExist());

    String oldPassword = user.getPassword();
    assertNotNull(oldPassword);
    Optional<User> updatedUser = userRepository.findById(user.getId());
    assertTrue(updatedUser.isPresent());
    assertEquals(oldPassword, updatedUser.get().getPassword());
  }

  @Test
  @DisplayName("GET /{id}: Non-admin reading another user's account gets 403")
  void getAccountByIdAsDifferentUserGetsForbidden() throws Exception {
    User admin = userRepository.findByUsername("admin").orElseThrow();
    mvc.perform(get("/api/account/" + admin.getId()).cookie(new Cookie(ACCESS_TOKEN, validToken)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type", equalTo(ProblemTypes.FORBIDDEN)))
        .andExpect(jsonPath("$.status", equalTo(403)))
        .andExpect(jsonPath("$.title", equalTo("Forbidden")))
        .andExpect(jsonPath("$.detail", equalTo("Access is denied")))
        .andExpect(jsonPath("$.instance", equalTo("/api/account/" + admin.getId())));
  }

  @Test
  @DisplayName("GET /{id}: User reading own account gets 200")
  void getAccountByIdAsOwnUserReturnsOk() throws Exception {
    mvc.perform(get("/api/account/" + user.getId()).cookie(new Cookie(ACCESS_TOKEN, validToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", equalTo(username)));
  }

  @Test
  @DisplayName("GET /{id}: Admin reading any user's account gets 200")
  void getAccountByIdAsAdminReturnsOk() throws Exception {
    String adminToken = tokenProvider.generateToken("admin", new Date());
    mvc.perform(get("/api/account/" + user.getId()).cookie(new Cookie(ACCESS_TOKEN, adminToken)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /all: Non-admin gets 403")
  void getAllAccountsAsNonAdminGetsForbidden() throws Exception {
    mvc.perform(get("/api/account/all").cookie(new Cookie(ACCESS_TOKEN, validToken)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /all: Admin gets 200")
  void getAllAccountsAsAdminReturnsOk() throws Exception {
    String adminToken = tokenProvider.generateToken("admin", new Date());
    mvc.perform(get("/api/account/all").cookie(new Cookie(ACCESS_TOKEN, adminToken)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST: Reject empty password on account update")
  void rejectEmptyPasswordOnAccountUpdate() throws Exception {
    String content =
        """
        {
        "username":"%s",
        "firstName":"NameChanged",
        "lastName":"NameChanged",
        "password":"",
        "administrator": false,
        "blocked": false
        }
        """
            .formatted(username);

    mvc.perform(
            post(URIConstants.ACCOUNT_URI)
                .cookie(new Cookie(ACCESS_TOKEN, validToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
        .andExpect(status().isBadRequest());

    Optional<User> unchangedUser = userRepository.findById(user.getId());
    assertTrue(unchangedUser.isPresent());
    assertNotNull(unchangedUser.get().getPassword());
  }
}
