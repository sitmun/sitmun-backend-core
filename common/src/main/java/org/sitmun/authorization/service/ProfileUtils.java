package org.sitmun.authorization.service;

import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ProfileUtils {

  private ProfileUtils() {
    // Utility class
  }

  static Optional<Profile> buildProfile(User user, String appId, String terrId) {
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

        List<CartographyPermission> permissions = getPermissions(user, application, territory);
        List<Cartography> layers = getLayers(permissions);
        List<Service> services = getServices(layers);
        List<Task> tasks = getTasks(user, application, territory);

        return Profile.builder()
          .territory(territory)
          .application(application)
          .groups(permissions)
          .layers(layers)
          .services(services)
          .tasks(tasks)
          .build();
      });
  }

  static Pair<Application, Territory> getApplicationAndTerritory(UserConfiguration userConfiguration, String appId) {
    return userConfiguration.getRole().getApplications().stream()
      .filter(app -> Objects.equals(app.getId(), Integer.valueOf(appId)))
      .findFirst()
      .map(value -> Pair.of(value, userConfiguration.getTerritory()))
      .orElse(null);
  }

  static List<CartographyPermission> getPermissions(User user, Application application, Territory territory) {
    return user.getPermissions().stream()
      .filter(permission -> permission.getRole().getApplications().stream().anyMatch(Predicate.isEqual(application)))
      .filter(permission -> Objects.equals(permission.getTerritory(), territory))
      .flatMap(permission -> permission.getRole().getPermissions().stream())
      .distinct()
      .collect(Collectors.toList());
  }

  static List<Cartography> getLayers(List<CartographyPermission> permissions) {
    return permissions.stream()
      .flatMap(cp -> cp.getMembers().stream())
      .distinct()
      .collect(Collectors.toList());
  }

  static List<Service> getServices(List<Cartography> layers) {
    return layers.stream()
      .map(Cartography::getService)
      .distinct()
      .collect(Collectors.toList());
  }

  static List<Task> getTasks(User user, Application application, Territory territory) {
    return user.getPermissions().stream()
      .filter(permission -> Objects.equals(permission.getTerritory(), territory))
      .map(UserConfiguration::getRole)
      .filter(role -> role.getApplications().stream().anyMatch(Predicate.isEqual(application)))
      .flatMap(role -> role.getTasks().stream())
      .distinct()
      .collect(Collectors.toList());
  }

  static Boolean isAppPartOfUserConfiguration(Integer appId, UserConfiguration uc) {
    return uc.getRole().getApplications().stream().anyMatch(app -> Objects.equals(app.getId(), appId));
  }

}
