package org.sitmun.verification.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.test.TestUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Verification Controller tests")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VerificationControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void cleanUp() {
    userRepository.findByUsername("admin").orElseThrow();
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
  }

  @AfterEach
  void cleanupTestUsers() {
    userRepository.findByUsername("testuser").ifPresent(userRepository::delete);
  }

  @Test
  @DisplayName("Verify password with valid credentials should return true")
  void verifyPasswordWithValidCredentials() throws Exception {
    UserPasswordAuthenticationRequest request = new UserPasswordAuthenticationRequest();
    request.setUsername("admin");
    request.setPassword("admin");

    mvc.perform(post("/api/user-verification/verify-password")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").value(true));
  }

  @Test
  @DisplayName("Verify password with invalid credentials should return false")
  void verifyPasswordWithInvalidCredentials() throws Exception {
    UserPasswordAuthenticationRequest request = new UserPasswordAuthenticationRequest();
    request.setUsername("admin");
    request.setPassword("wrongpassword");

    mvc.perform(post("/api/user-verification/verify-password")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$").value(false));
  }

  @Test
  @DisplayName("Verify password with empty username should return bad request")
  void verifyPasswordWithEmptyUsername() throws Exception {
    UserPasswordAuthenticationRequest request = new UserPasswordAuthenticationRequest();
    request.setUsername("");
    request.setPassword("admin");

    mvc.perform(post("/api/user-verification/verify-password")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Verify password with null password should return bad request")
  void verifyPasswordWithNullPassword() throws Exception {
    UserPasswordAuthenticationRequest request = new UserPasswordAuthenticationRequest();
    request.setUsername("admin");
    request.setPassword(null);

    mvc.perform(post("/api/user-verification/verify-password")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Verify email with existing email should return true")
  void verifyEmailWithExistingEmail() throws Exception {
    // Create a test user first
    User testUser = new User();
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("password");
    testUser.setAdministrator(false);
    testUser.setBlocked(false);
    userRepository.save(testUser);
    
    // Verify the user was saved
    assertThat(userRepository.findByEmail("test@example.com")).isPresent();

    String email = "test@example.com";

    mvc.perform(post("/api/user-verification/verify-email")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.TEXT_PLAIN)
        .content( email ))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").value(true));
  }

  @Test
  @DisplayName("Verify email with non-existing email should return false")
  void verifyEmailWithNonExistingEmail() throws Exception {
    String email = "nonexistent@example.com";

    mvc.perform(post("/api/user-verification/verify-email")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.TEXT_PLAIN)
        .content(email))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").value(false));
  }

  @Test
  @DisplayName("Verify email with empty email should return false")
  void verifyEmailWithEmptyEmail() throws Exception {
    mvc.perform(post("/api/user-verification/verify-email")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.TEXT_PLAIN)
        .content(""))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").value(false));
  }

  @Test
  @DisplayName("Verify email with null email should return false")
  void verifyEmailWithNullEmail() throws Exception {
    mvc.perform(post("/api/user-verification/verify-email")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("null"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").value(false));
  }

  @Test
  @DisplayName("Verify email should be case insensitive")
  void verifyEmailCaseInsensitive() throws Exception {
    // Create a test user with lowercase email
    User testUser = new User();
    testUser.setUsername("testuser");
    testUser.setEmail("testcase@example.com");
    testUser.setPassword("password");
    testUser.setAdministrator(false);
    testUser.setBlocked(false);
    userRepository.save(testUser);

    // Try with uppercase email
    String email = "TESTCASE@EXAMPLE.COM";

    mvc.perform(post("/api/user-verification/verify-email")
        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("USER"))
        .contentType(MediaType.TEXT_PLAIN)
        .content(email))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").value(true));
  }

  @Test
  @DisplayName("Unauthenticated requests should be rejected")
  void unauthenticatedRequestsShouldBeRejected() throws Exception {
    UserPasswordAuthenticationRequest request = new UserPasswordAuthenticationRequest();
    request.setUsername("admin");
    request.setPassword("admin");

    mvc.perform(post("/api/user-verification/verify-password")
        .contentType(MediaType.APPLICATION_JSON)
        .content(TestUtils.asJsonString(request)))
      .andExpect(status().isUnauthorized());
  }
} 