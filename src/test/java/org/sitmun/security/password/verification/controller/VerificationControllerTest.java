package org.sitmun.security.password.verification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.password.dto.PasswordVerificationRequest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Verification Controller tests")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VerificationControllerTest {

  @Autowired private MockMvc mvc;

  @Autowired private UserRepository userRepository;

  @BeforeEach
  void cleanUp() {
    userRepository.findByUsername("admin").orElseThrow();
    // Clean up test users with various usernames that might exist from previous test runs
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser-existing").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser-case").ifPresent(userRepository::delete);
  }

  @AfterEach
  void cleanupTestUsers() {
    // Clean up test users created during this test run
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser-existing").ifPresent(userRepository::delete);
    userRepository.findByUsername("testuser-case").ifPresent(userRepository::delete);
  }

  @Test
  @DisplayName("POST: Verify password with valid credentials should return true")
  @WithMockUser(username = "admin", roles = "USER")
  void verifyPasswordWithValidCredentials() throws Exception {
    checkPassword("admin", true);
  }

  @Test
  @DisplayName("POST: Verify password with invalid credentials should return false")
  @WithMockUser(username = "admin", roles = "USER")
  void verifyPasswordWithInvalidCredentials() throws Exception {
    checkPassword("wrongpassword", false);
  }

  @Test
  @DisplayName("POST: Verify password with empty password should return false")
  @WithMockUser(username = "admin", roles = "USER")
  void verifyPasswordWithEmptyPassword() throws Exception {
    checkPassword("", false);
  }

  void checkPassword(String password, boolean expectedResult) throws Exception {
    PasswordVerificationRequest request = new PasswordVerificationRequest();
    request.setPassword(password);

    mvc.perform(
            post("/api/user-verification/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(expectedResult));
  }

  @Test
  @DisplayName("POST: Verify password with null password should return bad request")
  @WithMockUser(username = "admin", roles = "USER")
  void verifyPasswordWithNullPassword() throws Exception {
    PasswordVerificationRequest request = new PasswordVerificationRequest();
    request.setPassword(null);

    mvc.perform(
            post("/api/user-verification/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Verify email with existing email should return true")
  @WithMockUser(roles = "USER")
  void verifyEmailWithExistingEmail() throws Exception {
    // Create a test user first with unique username
    User testUser = new User();
    testUser.setUsername("testuser-existing");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password");
    testUser.setAdministrator(false);
    testUser.setBlocked(false);
    userRepository.save(testUser);

    // Verify the user was saved
    assertThat(userRepository.findByEmail("test@example.com")).isPresent();

    String email = "test@example.com";

    mvc.perform(
            post("/api/user-verification/verify-email")
                .contentType(MediaType.TEXT_PLAIN)
                .content(email))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));
  }

  @Test
  @DisplayName("POST: Verify email with non-existing email should return false")
  @WithMockUser(roles = "USER")
  void verifyEmailWithNonExistingEmail() throws Exception {
    String email = "nonexistent@example.com";

    mvc.perform(
            post("/api/user-verification/verify-email")
                .contentType(MediaType.TEXT_PLAIN)
                .content(email))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));
  }

  @Test
  @DisplayName("POST: Verify email with empty email should return false")
  @WithMockUser(roles = "USER")
  void verifyEmailWithEmptyEmail() throws Exception {
    mvc.perform(
            post("/api/user-verification/verify-email")
                .contentType(MediaType.TEXT_PLAIN)
                .content(""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));
  }

  @Test
  @DisplayName("POST: Verify email with null email should return false")
  @WithMockUser(roles = "USER")
  void verifyEmailWithNullEmail() throws Exception {
    mvc.perform(
            post("/api/user-verification/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(false));
  }

  @Test
  @DisplayName("POST: Verify email should be case insensitive")
  @WithMockUser(roles = "USER")
  void verifyEmailCaseInsensitive() throws Exception {
    // Create a test user with lowercase email and unique username
    User testUser = new User();
    testUser.setUsername("testuser-case");
    testUser.setEmail("testcase@example.com");
    testUser.setPassword("password");
    testUser.setAdministrator(false);
    testUser.setBlocked(false);
    userRepository.save(testUser);

    // Try with uppercase email
    String email = "TESTCASE@EXAMPLE.COM";

    mvc.perform(
            post("/api/user-verification/verify-email")
                .contentType(MediaType.TEXT_PLAIN)
                .content(email))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(true));
  }

  @Test
  @DisplayName("POST: Unauthenticated requests should be rejected")
  void unauthenticatedRequestsShouldBeRejected() throws Exception {
    PasswordVerificationRequest request = new PasswordVerificationRequest();
    request.setPassword("admin");

    mvc.perform(
            post("/api/user-verification/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isUnauthorized());
  }
}
