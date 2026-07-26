package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.user.User;

@DisplayName("ApplicationPointOfContactPolicy")
class ApplicationPointOfContactPolicyTest {

  private User user(String username, boolean blocked, String email) {
    return User.builder()
        .id(1)
        .username(username)
        .blocked(blocked)
        .email(email)
        .administrator(true)
        .build();
  }

  @Test
  @DisplayName("null user is not eligible")
  void nullUserNotEligible() {
    assertThat(ApplicationPointOfContactPolicy.isEligible(null)).isFalse();
  }

  @Test
  @DisplayName("built-in public user is not eligible")
  void publicUserNotEligible() {
    assertThat(ApplicationPointOfContactPolicy.isEligible(user("public", false, "a@b.c")))
        .isFalse();
  }

  @Test
  @DisplayName("built-in admin user is not eligible")
  void adminUserNotEligible() {
    assertThat(ApplicationPointOfContactPolicy.isEligible(user("admin", false, "a@b.c"))).isFalse();
  }

  @Test
  @DisplayName("blocked ordinary user is not eligible")
  void blockedUserNotEligible() {
    assertThat(ApplicationPointOfContactPolicy.isEligible(user("alice", true, "alice@example.com")))
        .isFalse();
  }

  @Test
  @DisplayName("ordinary user without email is eligible")
  void ordinaryUserWithoutEmailIsEligible() {
    assertThat(ApplicationPointOfContactPolicy.isEligible(user("alice", false, null))).isTrue();
  }

  @Test
  @DisplayName("ordinary user with email is eligible")
  void ordinaryUserWithEmailIsEligible() {
    assertThat(
            ApplicationPointOfContactPolicy.isEligible(user("alice", false, "alice@example.com")))
        .isTrue();
  }

  @Test
  @DisplayName("null user has no publishable email")
  void nullUserHasNoPublishableEmail() {
    assertThat(ApplicationPointOfContactPolicy.hasPublishableEmail(null)).isFalse();
  }

  @Test
  @DisplayName("eligible user with blank email has no publishable email")
  void eligibleUserBlankEmailHasNoPublishableEmail() {
    assertThat(ApplicationPointOfContactPolicy.hasPublishableEmail(user("alice", false, "  ")))
        .isFalse();
  }

  @Test
  @DisplayName("eligible user with non-blank email has publishable email")
  void eligibleUserNonBlankEmailHasPublishableEmail() {
    assertThat(
            ApplicationPointOfContactPolicy.hasPublishableEmail(
                user("alice", false, "alice@example.com")))
        .isTrue();
  }

  @Test
  @DisplayName("ineligible blocked user has no publishable email even with non-blank email")
  void blockedUserWithEmailHasNoPublishableEmail() {
    assertThat(
            ApplicationPointOfContactPolicy.hasPublishableEmail(
                user("alice", true, "alice@example.com")))
        .isFalse();
  }
}
