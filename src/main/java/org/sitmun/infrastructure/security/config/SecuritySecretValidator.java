package org.sitmun.infrastructure.security.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails startup when security secrets are blank or shorter than {@value #MIN_SECRET_LENGTH}
 * characters.
 */
@Component
public class SecuritySecretValidator {

  private static final int MIN_SECRET_LENGTH = 32;

  private final String userSecret;
  private final String proxyMiddlewareSecret;

  public SecuritySecretValidator(
      @Value("${sitmun.user.secret}") String userSecret,
      @Value("${sitmun.proxy-middleware.secret}") String proxyMiddlewareSecret) {
    this.userSecret = userSecret;
    this.proxyMiddlewareSecret = proxyMiddlewareSecret;
  }

  @PostConstruct
  public void validate() {
    validateSecret("sitmun.user.secret", userSecret);
    validateSecret("sitmun.proxy-middleware.secret", proxyMiddlewareSecret);
  }

  static void validateSecret(String propertyName, String secret) {
    if (!StringUtils.hasText(secret)) {
      throw new IllegalStateException("%s must not be blank".formatted(propertyName));
    }
    if (secret.trim().length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "%s must be at least %d characters".formatted(propertyName, MIN_SECRET_LENGTH));
    }
  }
}
