package org.sitmun.infrastructure.security.storage;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

public interface PasswordStorage {
  void addPasswordStorage(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception;
}
