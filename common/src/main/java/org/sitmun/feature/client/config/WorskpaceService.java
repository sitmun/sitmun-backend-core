package org.sitmun.feature.client.config;

import com.google.common.collect.Lists;
import org.sitmun.common.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.domain.user.User;
import org.sitmun.common.domain.user.UserRepository;
import org.sitmun.common.domain.user.configuration.UserConfiguration;
import org.sitmun.common.domain.user.configuration.UserConfigurationRepository;
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
    Optional<User> user = userRepository.findByUsername(username);
    if (user.isPresent()) {
      User effectiveUser = user.get();
      if (!effectiveUser.getBlocked()) {
        return Optional.of(Workspace.builder()
          .territories(territories(effectiveUser))
          .config(Lists.newArrayList(configurationParameterRepository.findAll()))
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
