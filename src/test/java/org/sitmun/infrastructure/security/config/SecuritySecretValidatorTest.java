package org.sitmun.infrastructure.security.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecuritySecretValidatorTest {

  @Test
  void validateSecretRejectsBlankValue() {
    assertThatThrownBy(() -> SecuritySecretValidator.validateSecret("sitmun.user.secret", " "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sitmun.user.secret");
  }

  @Test
  void validateSecretRejectsShortValue() {
    assertThatThrownBy(
            () ->
                SecuritySecretValidator.validateSecret(
                    "sitmun.proxy-middleware.secret", "short-secret"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sitmun.proxy-middleware.secret");
  }

  @Test
  void validateSecretAcceptsMinimumLengthValue() {
    assertThatCode(
            () ->
                SecuritySecretValidator.validateSecret(
                    "sitmun.user.secret", "test-only-insecure-user-secret-32-bytes"))
        .doesNotThrowAnyException();
  }
}
