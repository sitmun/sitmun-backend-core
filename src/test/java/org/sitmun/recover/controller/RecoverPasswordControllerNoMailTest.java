package org.sitmun.recover.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user_token.UserToken;
import org.sitmun.domain.user_token.UserTokenRepository;
import org.sitmun.recover.dto.ResetPasswordRequest;
import org.sitmun.recover.dto.UserLoginRecoverRequest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:config/application-test.yml")
@ActiveProfiles("test")
@DisplayName("Password Recovery Controller Tests - Test Profile Only (No Mail)")
class RecoverPasswordControllerNoMailTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserTokenRepository userTokenRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User testUser;
  private String validToken;
  private String expiredToken;

  @BeforeEach
  void setUp() {
    // Clean up any existing test data
    userTokenRepository.deleteAll();
    userRepository.deleteAll();

    // Create test user
    testUser = User.builder()
      .username("testuser")
      .email("test@example.com")
      .firstName("Test")
      .lastName("User")
      .password("oldpassword")
      .administrator(false)
      .blocked(false)
      .build();
    // Let the UserEventHandler encode the password
    testUser = userRepository.save(testUser);

    // Create valid token
    validToken = UUID.randomUUID().toString();
    UserToken validUserToken = UserToken.builder()
      .userMail(testUser.getEmail())
      .tokenId(validToken)
      .expireAt(new Date(System.currentTimeMillis() + 300000)) // 5 minutes from now
      .build();
    userTokenRepository.save(validUserToken);

    // Create expired token
    expiredToken = UUID.randomUUID().toString();
    UserToken expiredUserToken = UserToken.builder()
      .userMail(testUser.getEmail())
      .tokenId(expiredToken)
      .expireAt(new Date(System.currentTimeMillis() - 300000)) // 5 minutes ago
      .build();
    userTokenRepository.save(expiredUserToken);
  }

  @Test
  @DisplayName("POST: Send recovery email should fail when mail profile is not active")
  void sendRecoveryEmailShouldFailWhenMailProfileNotActive() throws Exception {
    checkServiceUnavailable("test@example.com");
  }

  @Test
  @DisplayName("POST: Send recovery email with username should fail when mail profile is not active")
  void sendRecoveryEmailWithUsernameShouldFailWhenMailProfileNotActive() throws Exception {
    checkServiceUnavailable("testuser");
  }

  @Test
  @DisplayName("POST: Send recovery email with invalid login should still return for security")
  void sendRecoveryEmailWithInvalidLoginShouldStillReturnSuccessForSecurity() throws Exception {
    checkServiceUnavailable("nonexistent@example.com");
  }

  void checkServiceUnavailable(String login) throws Exception {
    UserLoginRecoverRequest request = new UserLoginRecoverRequest();
    request.setLogin(login);

    mvc.perform(post("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isServiceUnavailable())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("Mail service is not available")));

    assertThat(userTokenRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("POST: Send recovery email with empty login should still fail with bad request")
  void sendRecoveryEmailWithEmptyLoginShouldFailWithBadRequest() throws Exception {
    UserLoginRecoverRequest request = new UserLoginRecoverRequest();
    request.setLogin("");

    mvc.perform(post("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Send recovery email with null login should still fail with bad request")
  void sendRecoveryEmailWithNullLoginShouldFailWithBadRequest() throws Exception {
    UserLoginRecoverRequest request = new UserLoginRecoverRequest();
    request.setLogin(null);

    mvc.perform(post("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT: Reset password with valid token should succeed even without mail profile")
  void resetPasswordWithValidTokenShouldSucceedEvenWithoutMailProfile() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(validToken);
    request.setPassword("newpassword123");

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isOk())
      .andExpect(content().string("Password reset successfully"));

    // Verify password was updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(passwordEncoder.matches("newpassword123", updatedUser.get().getPassword())).isTrue();

    // Verify token was deleted
    Optional<UserToken> deletedToken = userTokenRepository.findByTokenId(validToken);
    assertThat(deletedToken).isEmpty();
  }

  @Test
  @DisplayName("PUT: Reset password with expired token should fail with server error")
  void resetPasswordWithExpiredTokenShouldFailWithServerError() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(expiredToken);
    request.setPassword("newpassword123");

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isInternalServerError())
      .andExpect(content().string("Server error"));

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getPassword()).isEqualTo("oldpassword");

    // Verify token was not deleted
    Optional<UserToken> token = userTokenRepository.findByTokenId(expiredToken);
    assertThat(token).isPresent();
  }

  @Test
  @DisplayName("PUT: Reset password with invalid token should fail with server error")
  void resetPasswordWithInvalidTokenShouldFailWithServerError() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("invalid-token");
    request.setPassword("newpassword123");

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isInternalServerError())
      .andExpect(content().string("Server error"));

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getPassword()).isEqualTo("oldpassword");
  }

  @Test
  @DisplayName("PUT: Reset password with empty password should still fail with bad request")
  void resetPasswordWithEmptyPasswordShouldFailWithBadRequest() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(validToken);
    request.setPassword("");

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getPassword()).isEqualTo("oldpassword");
  }

  @Test
  @DisplayName("PUT: Reset password with null password should still fail with bad request")
  void resetPasswordWithNullPasswordShouldFailWithBadRequest() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(validToken);
    request.setPassword(null);

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getPassword()).isEqualTo("oldpassword");
  }

  @Test
  @DisplayName("PUT: Reset password with null token should still fail with bad request")
  void resetPasswordWithNullTokenShouldFailWithBadRequest() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(null);
    request.setPassword("newpassword123");

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getPassword()).isEqualTo("oldpassword");
  }

  @Test
  @DisplayName("PUT: Reset password with very short password should succeed (no validation)")
  void resetPasswordWithVeryShortPasswordShouldSucceed() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(validToken);
    request.setPassword("123");

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isOk())
      .andExpect(content().string("Password reset successfully"));

    // Verify password was updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(passwordEncoder.matches("123", updatedUser.get().getPassword())).isTrue();
  }

  @Test
  @DisplayName("PUT: Reset password with very long password should still fail with bad request")
  void resetPasswordWithVeryLongPasswordShouldFailWithBadRequest() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken(validToken);
    request.setPassword("a".repeat(256)); // Very long password

    mvc.perform(put("/api/recover-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(updatedUser.get().getPassword()).isEqualTo("oldpassword");
  }
} 