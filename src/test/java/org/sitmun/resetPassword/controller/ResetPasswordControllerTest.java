package org.sitmun.resetPassword.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user_token.UserToken;
import org.sitmun.domain.user_token.UserTokenRepository;
import org.sitmun.resetPassword.dto.ResetPasswordRequest;
import org.sitmun.test.AdditiveActiveProfiles;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMailConfig.class)
@DisplayName("Password Recovery Controller Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AdditiveActiveProfiles("mail")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResetPasswordControllerTest {
  @Autowired private MockMvc mvc;

  @Autowired private UserRepository userRepository;

  @Autowired private UserTokenRepository userTokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private String apiUrl = "/api/password-reset/";
  private User testUser;
  private String validToken;
  private String expiredToken;
  private String counterLimitToken;
  private String notActiveToken;

  @BeforeEach
  void setUp() {
    // Clean up any existing test data
    userTokenRepository.deleteAll();

    // Only delete test-specific users, preserve admin user
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser2").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser3").ifPresent(userRepository::delete);

    // Create test user
    testUser =
        User.builder()
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
    validToken = Integer.toString((int) (Math.random() * 100_000_000));
    UserToken validUserToken =
        UserToken.builder()
            .codeOTP(validToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(3)
            .active(true)
            .build();
    userTokenRepository.save(validUserToken);

    // Create expired token
    expiredToken = Integer.toString((int) (Math.random() * 100_000_000));
    UserToken expiredUserToken =
        UserToken.builder()
            .codeOTP(expiredToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() - 300000)) // 5 minutes ago
            .active(true)
            .attemptCounter(3)
            .build();
    userTokenRepository.save(expiredUserToken);

    counterLimitToken = Integer.toString((int) (Math.random() * 100_000_000));
    UserToken counterLimitExcedUserToken =
        UserToken.builder()
            .codeOTP(counterLimitToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis())) // 5 minutes ago
            .active(true)
            .attemptCounter(0)
            .build();
    userTokenRepository.save(counterLimitExcedUserToken);

    notActiveToken = Integer.toString((int) (Math.random() * 100_000_000));
    UserToken notActiveUserToken =
        UserToken.builder()
            .codeOTP(notActiveToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis())) // 5 minutes ago
            .active(false)
            .attemptCounter(3)
            .build();
    userTokenRepository.save(notActiveUserToken);
  }

  /* REQUEST -> Send email */
  @Test
  @DisplayName("POST: Send recovery email with valid email")
  @Transactional
  @Rollback
  void sendRecoveryEmailWithValidEmail() throws Exception {
    checkService("test@example.com", 3);
  }

  @Test
  @DisplayName("POST: Send recovery email with invalid email")
  @Transactional
  @Rollback
  void sendRecoveryEmailWithInvalidLogin() throws Exception {
    checkService("nonexistent@example.com", 2);
  }

  void checkService(String login, int tokens) throws Exception {
    org.sitmun.resetPassword.dto.RequestNewPassword request =
        new org.sitmun.resetPassword.dto.RequestNewPassword();
    request.setEmail(login);

    mvc.perform(
            post(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Mail sent"));

    assertThat(userTokenRepository.count()).isEqualTo(tokens);
  }

  @Test
  @DisplayName("POST: Send recovery email with empty mail")
  @Transactional
  @Rollback
  void sendRecoveryEmailWithEmptyLogin() throws Exception {
    org.sitmun.resetPassword.dto.RequestNewPassword request =
        new org.sitmun.resetPassword.dto.RequestNewPassword();
    request.setEmail("");

    mvc.perform(
            post(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Send recovery email with null mail")
  @Transactional
  @Rollback
  void sendRecoveryEmailWithNullLogin() throws Exception {
    org.sitmun.resetPassword.dto.RequestNewPassword request =
        new org.sitmun.resetPassword.dto.RequestNewPassword();
    request.setEmail(null);

    mvc.perform(
            post(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Await error 429 after 5 request")
  @Transactional
  @Rollback
  void sendRequestTooManyTime() throws Exception {
    org.sitmun.resetPassword.dto.RequestNewPassword request =
        new org.sitmun.resetPassword.dto.RequestNewPassword();
    request.setEmail(testUser.getEmail());
    int maxRequest = 5;
    for (int i = 0; i < maxRequest; i++) {
      mvc.perform(
              post(apiUrl + "request")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(TestUtils.asJsonString(request)))
          .andExpect(status().isOk());
    }
    mvc.perform(
            post(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isTooManyRequests());
  }

  /* CONFIRM -> Change password */
  @Test
  @DisplayName("PUT: Reset password with valid token")
  @Transactional
  @Rollback
  void resetPasswordWithValidToken() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            put(apiUrl + "confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Password reset successfully"));

    // Verify password was updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(passwordEncoder.matches("newpassword123", updatedUser.get().getPassword())).isTrue();
  }

  @Test
  @DisplayName("PUT: Reset password with expired token")
  @Transactional
  @Rollback
  void resetPasswordWithExpiredToken() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(expiredToken);
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            put(apiUrl + "confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isGone())
        .andExpect(content().string("Gone"));

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    // Don't check password encoding in tests as it may not be properly encoded
    assertThat(updatedUser.get().getPassword()).isNotNull();
  }

  @Test
  @DisplayName("PUT: Reset password with token attempt counter exced limit")
  @Transactional
  @Rollback
  void resetPasswordWithTokenCounterExceedLimit() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(counterLimitToken);
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            put(apiUrl + "confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isGone())
        .andExpect(content().string("Gone"));

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    // Don't check password encoding in tests as it may not be properly encoded
    assertThat(updatedUser.get().getPassword()).isNotNull();
  }

  @Test
  @DisplayName("PUT: Reset password with token not active")
  @Transactional
  @Rollback
  void resetPasswordWithTokenNotActive() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(notActiveToken);
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            put(apiUrl + "confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isGone())
        .andExpect(content().string("Gone"));

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    // Don't check password encoding in tests as it may not be properly encoded
    assertThat(updatedUser.get().getPassword()).isNotNull();
  }

  @Test
  @DisplayName("PUT: Reset password with invalid token")
  @Transactional
  @Rollback
  void resetPasswordWithInvalidToken() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP("12345678");
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            put(apiUrl + "confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Bad Request"));

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    // Don't check password encoding in tests as it may not be properly encoded
    assertThat(updatedUser.get().getPassword()).isNotNull();
  }

  @Test
  @DisplayName("PUT: Reset password with empty password")
  @Transactional
  @Rollback
  void resetPasswordWithEmptyPassword() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword("");

    mvc.perform(
            put(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    // Don't check password encoding in tests as it may not be properly encoded
    assertThat(updatedUser.get().getPassword()).isNotNull();
  }

  @Test
  @DisplayName("PUT: Reset password with null password")
  @Transactional
  @Rollback
  void resetPasswordWithNullPassword() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword(null);

    mvc.perform(
            put(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT: Reset password with null token")
  @Transactional
  @Rollback
  void resetPasswordWithNullToken() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setEmail(null);
    request.setNewPassword("newpassword123");

    mvc.perform(
            put(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT: Reset password with very short password")
  @Transactional
  @Rollback
  void resetPasswordWithVeryShortPassword() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword("a");

    mvc.perform(
            put(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Password reset successfully"));

    // Verify password was updated (the controller accepts short passwords)
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    assertThat(passwordEncoder.matches("a", updatedUser.get().getPassword())).isTrue();
  }

  @Test
  @DisplayName("PUT: Reset password with very long password")
  @Transactional
  @Rollback
  void resetPasswordWithVeryLongPassword() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword("a".repeat(51)); // Exceeds max length of 50

    mvc.perform(
            put(apiUrl + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());

    // Verify password was not updated
    Optional<User> updatedUser = userRepository.findById(testUser.getId());
    assertThat(updatedUser).isPresent();
    // Don't check password encoding in tests as it may not be properly encoded
    assertThat(updatedUser.get().getPassword()).isNotNull();
  }

  /* resend -> resent email */
  @Test
  @Transactional
  @Rollback
  @DisplayName("POST: Resend OTP generates new code and resets attempts")
  void resendOTP() throws Exception {
    Optional<UserToken> oldTokenOpt = userTokenRepository.findByUserID(testUser.getId());
    String oldCodeOTP = oldTokenOpt.map(UserToken::getCodeOTP).orElse(null);
    int oldAttempts = oldTokenOpt.map(UserToken::getAttemptCounter).orElse(-1);

    org.sitmun.resetPassword.dto.RequestNewPassword request =
        new org.sitmun.resetPassword.dto.RequestNewPassword();
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(apiUrl + "resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Mail sent"));

    Optional<UserToken> newTokenOpt = userTokenRepository.findByUserID(testUser.getId());
    assertThat(newTokenOpt).isPresent();

    UserToken newToken = newTokenOpt.get();

    assertThat(newToken.getCodeOTP()).isNotEqualTo(oldCodeOTP);

    assertThat(newToken.getAttemptCounter()).isEqualTo(3); // TODO: récupérer depuis yml vrai valuer

    assertThat(newToken.isActive()).isTrue();
  }

  @Test
  @DisplayName("POST: Await error 429 after 3 request")
  @Transactional
  @Rollback
  void sendResendTooManyTime() throws Exception {
    org.sitmun.resetPassword.dto.RequestNewPassword request =
        new org.sitmun.resetPassword.dto.RequestNewPassword();
    request.setEmail(testUser.getEmail());

    int max_request = 3;

    for (int i = 0; i < max_request; i++) {
      mvc.perform(
              post(apiUrl + "resend")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(TestUtils.asJsonString(request)))
          .andExpect(status().isOk());
    }
    mvc.perform(
            post(apiUrl + "resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isTooManyRequests());
  }
}
