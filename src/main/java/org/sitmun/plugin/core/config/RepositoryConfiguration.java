package org.sitmun.plugin.core.config;

import org.sitmun.plugin.core.repository.handlers.CodeListValueEventHandler;
import org.sitmun.plugin.core.repository.handlers.UserEventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class RepositoryConfiguration {

  @Bean
  UserEventHandler userEventHandler(@Autowired PasswordEncoder passwordEncoder) {
    return new UserEventHandler(passwordEncoder);
  }

  @Bean
  CodeListValueEventHandler codeListValueEventHandler() {
    return new CodeListValueEventHandler();
  }
}
