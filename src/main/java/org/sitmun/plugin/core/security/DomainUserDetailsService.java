package org.sitmun.plugin.core.security;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService implements UserDetailsService {

  private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

  private final UserRepository userRepository;

  public DomainUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public UserDetails loadUserByUsername(final String login) {
    log.debug("Authenticating {}", login);
    String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
    Optional<User> userByEmailFromDatabase = userRepository.findOneByUsername(lowercaseLogin);
    return userByEmailFromDatabase.map(user -> createSpringSecurityUser(lowercaseLogin, user)).orElseGet(() -> {
      Optional<User> userByLoginFromDatabase = userRepository.findOneByUsername(lowercaseLogin);
      return userByLoginFromDatabase.map(user -> createSpringSecurityUser(lowercaseLogin, user))
                 .orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the " +
                                                                      "database"));
    });
  }

  private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user) {
    return new org.springframework.security.core.userdetails.User(user.getUsername(),
        user.getPassword(), new ArrayList<>());
  }
}
