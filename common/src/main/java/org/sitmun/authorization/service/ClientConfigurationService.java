package org.sitmun.authorization.service;

import com.google.common.collect.Lists;
import org.sitmun.authorization.dto.ClientConfiguration;
import org.sitmun.authorization.dto.ClientConfigurationForApplicationTerritory;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ClientConfigurationService {

  private final UserRepository userRepository;

  private final UserConfigurationRepository userConfigurationRepository;

  private final ConfigurationParameterRepository configurationParameterRepository;

  /**
   * Constructor.
   */
  public ClientConfigurationService(
    UserRepository userRepository,
    UserConfigurationRepository userConfigurationRepository,
    ConfigurationParameterRepository configurationParameterRepository) {
    this.userRepository = userRepository;
    this.userConfigurationRepository = userConfigurationRepository;
    this.configurationParameterRepository = configurationParameterRepository;
  }

  public Optional<ClientConfigurationForApplicationTerritory> describeFor(String username, Integer applicationId, Integer territoryId) {
    Optional<User> user = userRepository.findByUsername(username);
    if (user.isPresent()) {
      User effectiveUser = user.get();
      if (Boolean.FALSE.equals(effectiveUser.getBlocked())) {
        List<Territory> territories = territoriesList(effectiveUser, applicationId, territoryId);
        if (!territories.isEmpty()) {
          Territory territory = territories.get(0);
          Optional<Application> application = territory.getUserConfigurations().stream()
            .flatMap(uc -> uc.getRole().getApplications().stream())
            .filter(app -> Objects.equals(app.getId(), applicationId))
            .findFirst();
          List<Role> roles = territory.getUserConfigurations().stream()
            .map(UserConfiguration::getRole)
            .map(role -> {
              Set<Task> tasks = role.getTasks().stream().filter(task ->
                  task.getAvailabilities().stream()
                    .anyMatch(avail -> Objects.equals(avail.getTerritory().getId(), territory.getId())))
                .collect(Collectors.toSet());
              Set<CartographyPermission> permissions = role.getPermissions().stream().map(cartographyPermission -> {
                    Set<Cartography> cartographies = cartographyPermission.getMembers().stream().filter(cartography ->
                      !cartography.getBlocked() &&
                        cartography.getAvailabilities().stream()
                          .anyMatch(avail -> Objects.equals(avail.getTerritory().getId(), territory.getId()))
                    ).collect(Collectors.toSet());
                    return cartographyPermission.toBuilder().members(cartographies).build();
                  }
                ).filter(cartographyPermission -> !cartographyPermission.getMembers().isEmpty())
                .collect(Collectors.toSet());
              Set<Tree> trees = role.getTrees().stream().map(tree -> {
                Set<TreeNode> nodes = tree.getAllNodes().stream().filter(node ->
                  node.getCartographyId() == null
                    || (!node.getCartography().getBlocked() &&
                    node.getCartography().getAvailabilities().stream()
                      .anyMatch(avail -> Objects.equals(avail.getTerritory().getId(), territory.getId())) &&
                    permissions.stream().anyMatch(permission -> permission.getMembers().contains(node.getCartography()))
                  )
                ).collect(Collectors.toSet());
                return tree.toBuilder().allNodes(nodes).build();
              }).collect(Collectors.toSet());
              return role.toBuilder()
                .tasks(tasks)
                .permissions(permissions)
                .trees(trees).build();
            })
            .collect(Collectors.toList());
          if (application.isPresent()) {
            return Optional.of(ClientConfigurationForApplicationTerritory.builder()
              .territory(territories.get(0))
              .application(application.get())
              .roles(roles)
              .config(Lists.newArrayList(configurationParameterRepository.findAll()))
              .build());
          }
        }
      }
    }
    return Optional.empty();
  }

  private Stream<Territory> territoriesStream(User user) {
    return userConfigurationRepository.findByUser(user).stream()
      .map(UserConfiguration::getTerritory)
      .filter(territory -> !territory.getBlocked())
      .distinct();
  }


  private List<Territory> territoriesList(User user, Integer applicationId, Integer territoryId) {
    return territoriesStream(user)
      .map(territory -> {
        Set<UserConfiguration> filtered = territory.getUserConfigurations()
          .stream()
          .filter(uc -> uc.getUser() == user &&
            Objects.equals(uc.getTerritory().getId(), territoryId) &&
            uc.getRole().getApplications().stream().anyMatch(app -> Objects.equals(app.getId(), applicationId))
          )
          .collect(Collectors.toSet());
        if (filtered.isEmpty()) {
          return null;
        } else {
          return territory.toBuilder().userConfigurations(filtered).build();
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public Optional<ClientConfiguration> describeFor(String username) {
    Optional<User> user = userRepository.findByUsername(username);
    if (user.isPresent()) {
      User effectiveUser = user.get();
      if (Boolean.FALSE.equals(effectiveUser.getBlocked())) {
        return Optional.of(ClientConfiguration.builder()
          .territories(territoriesList(effectiveUser))
          .config(Lists.newArrayList(configurationParameterRepository.findAll()))
          .build());
      }
    }
    return Optional.empty();
  }

  private List<Territory> territoriesList(User user) {
    return userConfigurationRepository.findByUser(user).stream()
      .map(UserConfiguration::getTerritory)
      .filter(territory -> !territory.getBlocked())
      .distinct()
      .map(territory -> {
        Set<UserConfiguration> filtered = territory.getUserConfigurations()
          .stream()
          .filter(uc -> uc.getUser() == user)
          .collect(Collectors.toSet());
        return territory.toBuilder().userConfigurations(filtered).build();
      })
      .collect(Collectors.toList());
  }

  public Page<Application> applicationsPage(String username, Pageable pageable) {
    return userRepository.findByUsername(username)
      .filter(user -> Boolean.FALSE.equals(user.getBlocked()))
      .map(user -> {
        List<Application> applications = applicationsList(user);
        final int start = Math.min((int) pageable.getOffset(), applications.size());
        final int end = Math.min((start + pageable.getPageSize()), applications.size());
        return new PageImpl<>(applications.subList(start, end), pageable, applications.size());
      })
      .orElse(new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  public Page<Application> applicationsPage(Integer terrId, String username, Pageable pageable) {
    return userRepository.findByUsername(username)
      .filter(user -> Boolean.FALSE.equals(user.getBlocked()))
      .map(user -> {
        List<Application> applications = applicationsList(terrId, user);
        final int start = Math.min((int) pageable.getOffset(), applications.size());
        final int end = Math.min((start + pageable.getPageSize()), applications.size());
        return new PageImpl<>(applications.subList(start, end), pageable, applications.size());
      })
      .orElse(new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  private List<Application> applicationsList(User user) {
    return userConfigurationRepository.findByUser(user).stream()
      .map(UserConfiguration::getRole)
      .distinct()
      .map(Role::getApplications)
      .flatMap(Set::stream)
      .distinct()
      .collect(Collectors.toList());
  }

  private List<Application> applicationsList(Integer terrId, User user) {
    return userConfigurationRepository.findByUser(user).stream()
      .filter(uc -> Objects.equals(uc.getTerritory().getId(), terrId))
      .map(UserConfiguration::getRole)
      .distinct()
      .map(Role::getApplications)
      .flatMap(Set::stream)
      .distinct()
      .collect(Collectors.toList());
  }

  public Page<Territory> territoriesPage(String username, Pageable pageable) {
    return userRepository.findByUsername(username)
      .filter(user -> Boolean.FALSE.equals(user.getBlocked()))
      .map(user -> {
        List<Territory> territories = userConfigurationRepository.findByUser(user).stream()
          .map(UserConfiguration::getTerritory)
          .distinct()
          .collect(Collectors.toList());

        final int start = Math.min((int) pageable.getOffset(), territories.size());
        final int end = Math.min((start + pageable.getPageSize()), territories.size());
        return new PageImpl<>(territories.subList(start, end), pageable, territories.size());
      }).orElse(new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  /**
   * Get the profile for the given user, application and territory.
   *
   * @param username the username
   * @param appId    the application id
   * @param terrId   the territory id
   * @return the profile
   */
  public Optional<Profile> buildProfile(String username, String appId, String terrId) {
    return userRepository.findByUsername(username)
      .filter(user -> !user.getBlocked())
      .flatMap(user -> buildProfile(user, appId, terrId));
  }

  private Optional<Profile> buildProfile(User user, String appId, String terrId) {
    return user.getPermissions()
      .stream()
      .filter(it -> Objects.equals(it.getTerritory().getId(), Integer.valueOf(terrId)))
      .filter(it -> !it.getTerritory().getBlocked())
      .map(it -> getApplicationAndTerritory(it, appId))
      .filter(Objects::nonNull)
      .findFirst()
      .map(it -> {
        Application application = it.getFirst();
        Territory territory = it.getSecond();
        return buildProfile(user, application, territory);
      });
  }

  private static Profile buildProfile(User user, Application application, Territory territory) {

    List<CartographyPermission> permissions = getPermissions(user, application, territory);
    List<Cartography> layers = getLayers(permissions);
    List<org.sitmun.domain.service.Service> services = getServices(layers);
    List<Task> tasks = getTasks(user, application, territory);

    return Profile.builder()
      .territory(territory)
      .application(application)
      .groups(permissions)
      .layers(layers)
      .services(services)
      .tasks(tasks)
      .build();
  }

  private static List<CartographyPermission> getPermissions(User user, Application application, Territory territory) {
    return user.getPermissions().stream()
      .filter(permission -> permission.getRole().getApplications().stream().anyMatch(app -> Objects.equals(app, application)))
      .filter(permission -> Objects.equals(permission.getTerritory(), territory))
      .flatMap(permission -> permission.getRole().getPermissions().stream())
      .distinct()
      .collect(Collectors.toList());
  }

  private static List<Cartography> getLayers(List<CartographyPermission> permissions) {
    return permissions.stream()
      .flatMap(cp -> cp.getMembers().stream())
      .distinct()
      .collect(Collectors.toList());
  }

  private static List<org.sitmun.domain.service.Service> getServices(List<Cartography>  layers) {
    return layers.stream()
      .map(Cartography::getService)
      .distinct()
      .collect(Collectors.toList());
  }


  private static List<Task> getTasks(User user, Application application, Territory territory) {
    return user.getPermissions().stream()
      .filter(permission -> Objects.equals(permission.getTerritory(), territory))
      .map(UserConfiguration::getRole)
      .filter(role -> role.getApplications().stream().anyMatch(app -> Objects.equals(app, application)))
      .flatMap(role -> role.getTasks().stream())
      .distinct()
      .collect(Collectors.toList());
  }

  private Pair<Application, Territory> getApplicationAndTerritory(UserConfiguration userConfiguration, String appId) {
    return userConfiguration.getRole().getApplications().stream()
      .filter(app -> Objects.equals(app.getId(), Integer.valueOf(appId)))
      .findFirst()
      .map(value -> Pair.of(value, userConfiguration.getTerritory()))
      .orElse(null);
  }

}
