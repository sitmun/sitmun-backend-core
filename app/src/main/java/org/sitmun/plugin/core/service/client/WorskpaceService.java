package org.sitmun.plugin.core.service.client;

import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.domain.Workspace;
import org.sitmun.plugin.core.repository.ConfigurationParameterRepository;
import org.sitmun.plugin.core.repository.UserConfigurationRepository;
import org.sitmun.plugin.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorskpaceService {

  private final UserRepository userRepository;

  private final UserConfigurationRepository userConfigurationRepository;

  private final ConfigurationParameterRepository configurationParameterRepository;

  /**
   * Constructor.
   */
  public WorskpaceService(
    UserRepository userRepository,
    UserConfigurationRepository userConfigurationRepository,
    ConfigurationParameterRepository configurationParameterRepository) {
    this.userRepository = userRepository;
    this.userConfigurationRepository = userConfigurationRepository;
    this.configurationParameterRepository = configurationParameterRepository;
  }

  public Optional<Workspace> describeFor(String username) {
    Optional<User> user = userRepository.findOneByUsername(username);
    if (user.isPresent()) {
      User effectiveUser = user.get();
      if (!effectiveUser.getBlocked()) {
        return Optional.of(Workspace.builder()
          .territories(territories(effectiveUser))
          .config(configurationParameterRepository.findAll())
          .build());
      }
    }
    return Optional.empty();
  }

  private List<Territory> territories(User user) {
    return userConfigurationRepository.findByUser(user).stream()
      .map(UserConfiguration::getTerritory)
      .filter((territory) -> !territory.getBlocked())
      .distinct()
      .map((territory) -> {
        Set<UserConfiguration> filtered = territory.getUserConfigurations()
          .stream()
          .filter((uc) -> uc.getUser() == user)
          .collect(Collectors.toSet());
        return territory.toBuilder().userConfigurations(filtered).build();
      }).collect(Collectors.toList());
  }
}
