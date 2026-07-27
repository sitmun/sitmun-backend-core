package org.sitmun.infrastructure.startup;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sitmun.startup.built-in-users")
public class BuiltInUsersStartupProperties {

  private String adminPassword;

  public String getAdminPassword() {
    return adminPassword;
  }

  public void setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
  }

  /** Returns a non-blank bootstrap password, or empty when unset. */
  public Optional<String> normalizedAdminPassword() {
    return Optional.ofNullable(StringUtils.trimToNull(adminPassword));
  }

  @Override
  public String toString() {
    return "BuiltInUsersStartupProperties{adminPasswordConfigured="
        + normalizedAdminPassword().isPresent()
        + '}';
  }
}
