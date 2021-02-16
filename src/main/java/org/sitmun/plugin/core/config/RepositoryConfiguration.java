package org.sitmun.plugin.core.config;

import org.sitmun.plugin.core.repository.UserRepository;
import org.sitmun.plugin.core.repository.handlers.UserEventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class RepositoryConfiguration {

  @Bean
  UserEventHandler userEventHandler(@Autowired BCryptPasswordEncoder passwordEncoder,
                                    @Autowired UserRepository userRepository) {
    return new UserEventHandler(passwordEncoder, userRepository);
  }
}
