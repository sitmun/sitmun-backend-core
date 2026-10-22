package org.sitmun.infrastructure.security.password.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.token.UserToken;
import org.sitmun.domain.user.token.UserTokenRepository;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.infrastructure.security.password.dto.RequestNewPassword;
import org.sitmun.infrastructure.security.password.dto.ResetPasswordRequest;
import org.sitmun.infrastructure.security.password.service.CodeOTPService;
import org.sitmun.test.AdditiveActiveProfiles;
import org.sitmun.test.TestUtils;
import org.sitmun.test.UniqueContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MockMailConfig.class)
@DisplayName("Password Recovery Controller Tests")
@UniqueContext(
    "Password reset asserts mail sent through the mail profile and a mock JavaMailSender.")
@AdditiveActiveProfiles(Profiles.MAIL)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResetPasswordControllerTest {
  @Autowired private MockMvc mvc;

  @Autowired private UserRepository userRepository;

  @Autowired private UserTokenRepository userTokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private CodeOTPService codeOTPService;

  private static final String API_URL = "/api/password-reset/";
  private static final AtomicInteger CONFIRM_CLIENTS = new AtomicInteger();
  private User testUser;
  private String validToken;
  private String expiredToken;
  private String counterLimitToken;
  private String notActiveToken;

  /**
   * Unique email per test run to avoid IncorrectResultSizeDataAccessException when DB is shared
   * across contexts.
   */
  private String testUserEmail;

  private String confirmClientAddress;

  @BeforeEach
  void setUp() {
    confirmClientAddress = "10.7.0." + (CONFIRM_CLIENTS.incrementAndGet() % 200 + 1);
    // Clean up any existing test data
    userTokenRepository.deleteAll();

    // Only delete test-specific users, preserve admin user
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser2").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser3").ifPresent(userRepository::delete);

    // Unique email per test run so findByEmail returns a single result when DB is shared (e.g.
    // parallel contexts)
    testUserEmail = "test-" + System.nanoTime() + "@example.com";

    // Create test user
    testUser =
        User.builder()
            .username("testuser")
            .email(testUserEmail)
            .firstName("Test")
            .lastName("User")
            .password("oldpassword")
            .administrator(false)
            .blocked(false)
            .build();
    // Let the UserEventHandler encode the password
    testUser = userRepository.save(testUser);

    // Generate token values for different test scenarios (tokens will be created per test as
    // needed)
    Random random = new Random();
    validToken = Integer.toString(random.nextInt(100_000_000));

    // Store token values for different test scenarios (these will be created per test)
    expiredToken = Integer.toString(random.nextInt(100_000_000));
    counterLimitToken = Integer.toString(random.nextInt(100_000_000));
    notActiveToken = Integer.toString(random.nextInt(100_000_000));
  }

  @AfterEach
  void tearDown() {
    userTokenRepository.deleteAll();
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser2").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser3").ifPresent(userRepository::delete);
  }

  @Test
  @DisplayName("POST: Send recovery email with valid email")
  @Transactional
  void sendRecoveryEmailWithValidEmail() throws Exception {
    checkService(testUser.getEmail(), true);
  }

  @Test
  @DisplayName("Unknown address returns 200 and creates no token")
  @Transactional
  void sendRecoveryEmailWithInvalidLogin() throws Exception {
    checkService("nonexistent@example.com", false);
  }

  void checkService(String login, boolean shouldCreateToken) throws Exception {
    RequestNewPassword request = new RequestNewPassword();
    request.setEmail(login);

    long initialTokenCount = userTokenRepository.count();

    mvc.perform(
            post(API_URL + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Mail sent"));

    long finalTokenCount = userTokenRepository.count();

    if (shouldCreateToken) {
      assertThat(finalTokenCount).isEqualTo(initialTokenCount + 1);
    } else {
      assertThat(finalTokenCount).isEqualTo(initialTokenCount);
    }
  }

  @Test
  @DisplayName("Empty email returns 400")
  @Transactional
  void sendRecoveryEmailWithEmptyLogin() throws Exception {
    RequestNewPassword request = new RequestNewPassword();
    request.setEmail("");

    mvc.perform(
            post(API_URL + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Null email returns 400")
  @Transactional
  void sendRecoveryEmailWithNullLogin() throws Exception {
    RequestNewPassword request = new RequestNewPassword();
    request.setEmail(null);

    mvc.perform(
            post(API_URL + "request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Sixth password-reset request from one address returns 429")
  void sendRequestTooManyTime() throws Exception {
    userTokenRepository.deleteAll();

    RequestNewPassword request = new RequestNewPassword();
    request.setEmail(testUser.getEmail());
    int maxRequest = 5;

    for (int i = 0; i < maxRequest; i++) {
      mvc.perform(
              post(API_URL + "request")
                  .with(
                      httpRequest -> {
                        httpRequest.setRemoteAddr("10.0.0.1");
                        return httpRequest;
                      })
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(TestUtils.asJsonString(request)))
          .andExpect(status().isOk());
    }

    mvc.perform(
            post(API_URL + "request")
                .with(
                    httpRequest -> {
                      httpRequest.setRemoteAddr("10.0.0.1");
                      return httpRequest;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  @DisplayName("POST: Reset password with valid token")
  @Transactional
  void resetPasswordWithValidToken() throws Exception {
    // Create a valid token for this test - we need to hash the token like the controller does
    String hashedToken = codeOTPService.hashCodeOTP(validToken);
    UserToken validUserToken =
        UserToken.builder()
            .codeOTP(hashedToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(0)
            .active(true)
            .build();
    userTokenRepository.save(validUserToken);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken); // Send plain token, controller will hash and compare
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
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
  @DisplayName("POST: Reset password with expired token")
  @Transactional
  void resetPasswordWithExpiredToken() throws Exception {
    userTokenRepository.deleteAll();
    UserToken expiredUserToken =
        UserToken.builder()
            .codeOTP(expiredToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() - 300000)) // 5 minutes ago
            .active(true)
            .attemptCounter(3)
            .build();
    userTokenRepository.save(expiredUserToken);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(expiredToken);
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());
    String storedPassword = userRepository.findById(testUser.getId()).orElseThrow().getPassword();

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isGone());

    assertThat(userRepository.findById(testUser.getId()))
        .isPresent()
        .hasValueSatisfying(user -> assertThat(user.getPassword()).isEqualTo(storedPassword));
  }

  @Test
  @DisplayName("POST: Reset password with token attempt counter exceeded limit")
  @Transactional
  void resetPasswordWithTokenCounterExceedLimit() throws Exception {
    userTokenRepository.deleteAll();
    UserToken counterLimitExcedUserToken =
        UserToken.builder()
            .codeOTP(counterLimitToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .active(true)
            .attemptCounter(3) // Exceeded limit (max is 2)
            .build();
    userTokenRepository.save(counterLimitExcedUserToken);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(counterLimitToken);
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());
    String storedPassword = userRepository.findById(testUser.getId()).orElseThrow().getPassword();

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isGone());

    assertThat(userRepository.findById(testUser.getId()))
        .isPresent()
        .hasValueSatisfying(user -> assertThat(user.getPassword()).isEqualTo(storedPassword));
  }

  @Test
  @DisplayName("POST: Reset password with token not active")
  @Transactional
  void resetPasswordWithTokenNotActive() throws Exception {
    userTokenRepository.deleteAll();
    UserToken notActiveUserToken =
        UserToken.builder()
            .codeOTP(notActiveToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .active(false) // Not active
            .attemptCounter(3)
            .build();
    userTokenRepository.save(notActiveUserToken);
    String storedPassword = userRepository.findById(testUser.getId()).orElseThrow().getPassword();

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(notActiveToken);
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isGone())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(jsonPath("$.title").value("Gone"))
        .andExpect(jsonPath("$.detail").value("Token has been deactivated"));

    assertThat(userRepository.findById(testUser.getId()))
        .isPresent()
        .hasValueSatisfying(user -> assertThat(user.getPassword()).isEqualTo(storedPassword));
  }

  @Test
  @DisplayName("Wrong OTP returns 400, body 2, and leaves the password unchanged")
  @Transactional
  void resetPasswordWithInvalidToken() throws Exception {
    UserToken validUserToken =
        UserToken.builder()
            .codeOTP(validToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(0)
            .active(true)
            .build();
    userTokenRepository.save(validUserToken);
    String storedPassword = userRepository.findById(testUser.getId()).orElseThrow().getPassword();

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP("12345678"); // Invalid OTP
    request.setNewPassword("newpassword123");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("2")); // 3 max attempts - 1 current attempt = 2 remaining

    assertThat(userRepository.findById(testUser.getId()))
        .isPresent()
        .hasValueSatisfying(user -> assertThat(user.getPassword()).isEqualTo(storedPassword));
  }

  @Test
  @DisplayName("Unknown email confirm matches a wrong code and a known user with no code")
  @Transactional
  void confirmUnknownEmailMatchesKnownUserFailure() throws Exception {
    String hashedToken = codeOTPService.hashCodeOTP(validToken);
    userTokenRepository.save(
        UserToken.builder()
            .codeOTP(hashedToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(0)
            .active(true)
            .build());

    User userWithoutCode =
        userRepository.save(
            User.builder()
                .username("testuser2")
                .email("test2-" + System.nanoTime() + "@example.com")
                .firstName("Test")
                .lastName("Two")
                .password("oldpassword")
                .administrator(false)
                .blocked(false)
                .build());

    MvcResult wrongCode = postConfirm(resetRequest(testUser.getEmail(), "12345678"));
    MvcResult unknown =
        postConfirm(resetRequest("missing-" + System.nanoTime() + "@example.com", "12345678"));
    MvcResult noCode = postConfirm(resetRequest(userWithoutCode.getEmail(), "12345678"));

    assertThat(wrongCode.getResponse().getStatus()).isEqualTo(400);
    assertThat(wrongCode.getResponse().getContentAsString()).isEqualTo("2");
    assertThat(unknown.getResponse().getStatus()).isEqualTo(wrongCode.getResponse().getStatus());
    assertThat(unknown.getResponse().getContentAsString())
        .isEqualTo(wrongCode.getResponse().getContentAsString());
    assertThat(noCode.getResponse().getStatus()).isEqualTo(wrongCode.getResponse().getStatus());
    assertThat(noCode.getResponse().getContentAsString())
        .isEqualTo(wrongCode.getResponse().getContentAsString());
    assertThat(userTokenRepository.findByUserID(userWithoutCode.getId())).isEmpty();
  }

  @Test
  @DisplayName("POST: Reset password with empty password")
  @Transactional
  void resetPasswordWithEmptyPassword() throws Exception {
    UserToken validUserToken =
        UserToken.builder()
            .codeOTP(validToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(0)
            .active(true)
            .build();
    userTokenRepository.save(validUserToken);
    String storedPassword = userRepository.findById(testUser.getId()).orElseThrow().getPassword();

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword("");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());

    assertThat(userRepository.findById(testUser.getId()))
        .isPresent()
        .hasValueSatisfying(user -> assertThat(user.getPassword()).isEqualTo(storedPassword));
  }

  @Test
  @DisplayName("POST: Reset password with null password")
  @Transactional
  void resetPasswordWithNullPassword() throws Exception {
    UserToken validUserToken =
        UserToken.builder()
            .codeOTP(validToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(0)
            .active(true)
            .build();
    userTokenRepository.save(validUserToken);

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword(null);
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Reset password with null token")
  @Transactional
  void resetPasswordWithNullToken() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(null);
    request.setEmail(testUser.getEmail());
    request.setNewPassword("newpassword123");

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Reset password with very short password")
  @Transactional
  void resetPasswordWithVeryShortPassword() throws Exception {
    UserToken validUserToken =
        UserToken.builder()
            .codeOTP(validToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(0)
            .active(true)
            .build();
    userTokenRepository.save(validUserToken);
    String storedPassword = userRepository.findById(testUser.getId()).orElseThrow().getPassword();

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword("a");
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());

    assertThat(userRepository.findById(testUser.getId()))
        .isPresent()
        .hasValueSatisfying(user -> assertThat(user.getPassword()).isEqualTo(storedPassword));
  }

  @Test
  @DisplayName("POST: Reset password with very long password")
  @Transactional
  void resetPasswordWithVeryLongPassword() throws Exception {
    UserToken validUserToken =
        UserToken.builder()
            .codeOTP(validToken)
            .userID(testUser.getId())
            .expireAt(new Date(System.currentTimeMillis() + 300000))
            .attemptCounter(0)
            .active(true)
            .build();
    userTokenRepository.save(validUserToken);
    String storedPassword = userRepository.findById(testUser.getId()).orElseThrow().getPassword();

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setCodeOTP(validToken);
    request.setNewPassword("a".repeat(51)); // Exceeds max length of 50
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());

    assertThat(userRepository.findById(testUser.getId()))
        .isPresent()
        .hasValueSatisfying(user -> assertThat(user.getPassword()).isEqualTo(storedPassword));
  }

  @Test
  @Transactional
  @DisplayName("POST: Resend OTP generates new code and resets attempts")
  void resendOTP() throws Exception {
    // Get the existing token from setup
    Optional<UserToken> oldTokenOpt = userTokenRepository.findByUserID(testUser.getId());
    String oldCodeOTP = oldTokenOpt.map(UserToken::getCodeOTP).orElse(null);

    RequestNewPassword request = new RequestNewPassword();
    request.setEmail(testUser.getEmail());

    mvc.perform(
            post(API_URL + "resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Mail sent"));

    Optional<UserToken> newTokenOpt = userTokenRepository.findByUserID(testUser.getId());
    assertThat(newTokenOpt).isPresent();

    UserToken newToken = newTokenOpt.get();

    assertThat(newToken.getCodeOTP()).isNotEqualTo(oldCodeOTP);

    assertThat(newToken.getAttemptCounter()).isZero(); // New token starts with 0 attempts

    assertThat(newToken.isActive()).isTrue();
  }

  @Test
  @DisplayName("Fourth resend from one address returns 429")
  void sendResendTooManyTime() throws Exception {
    userTokenRepository.deleteAll();

    RequestNewPassword request = new RequestNewPassword();
    request.setEmail(testUser.getEmail());

    int maxRequest = 3;

    for (int i = 0; i < maxRequest; i++) {
      mvc.perform(
              post(API_URL + "resend")
                  .with(
                      httpRequest -> {
                        httpRequest.setRemoteAddr("10.0.0.2");
                        return httpRequest;
                      })
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(TestUtils.asJsonString(request)))
          .andExpect(status().isOk());
    }

    mvc.perform(
            post(API_URL + "resend")
                .with(
                    httpRequest -> {
                      httpRequest.setRemoteAddr("10.0.0.2");
                      return httpRequest;
                    })
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  @DisplayName("Sixth password-reset confirm from one address returns 429")
  void confirmTooManyTimes() throws Exception {
    ResetPasswordRequest request =
        resetRequest("missing-" + System.nanoTime() + "@example.com", "12345678");
    RequestPostProcessor remote = remoteAddress("10.0.0.3");

    for (int i = 0; i < 5; i++) {
      int status =
          mvc.perform(
                  post(API_URL + "confirm")
                      .with(remote)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(TestUtils.asJsonString(request)))
              .andReturn()
              .getResponse()
              .getStatus();
      assertThat(status).isNotEqualTo(429);
    }

    mvc.perform(
            post(API_URL + "confirm")
                .with(remote)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isTooManyRequests());
  }

  private MvcResult postConfirm(ResetPasswordRequest request) throws Exception {
    return mvc.perform(
            post(API_URL + "confirm")
                .with(remoteAddress(confirmClientAddress))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andReturn();
  }

  private static ResetPasswordRequest resetRequest(String email, String code) {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setEmail(email);
    request.setCodeOTP(code);
    request.setNewPassword("newpassword123");
    return request;
  }

  private static RequestPostProcessor remoteAddress(String address) {
    return httpRequest -> {
      httpRequest.setRemoteAddr(address);
      return httpRequest;
    };
  }
}
