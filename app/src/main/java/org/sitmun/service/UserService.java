package org.sitmun.service;


import org.sitmun.domain.Territory;
import org.sitmun.domain.User;
import org.sitmun.domain.UserConfiguration;
import org.sitmun.repository.UserRepository;
import org.sitmun.security.AuthoritiesConstants;
import org.sitmun.security.PermissionResolver;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements PermissionResolver<User> {

  private final UserRepository applicationUserRepository;

  public UserService(UserRepository applicationUserRepository) {
    super();
    this.applicationUserRepository = applicationUserRepository;
  }

  public boolean resolvePermission(User authUser, User user, String permission) {
    if (authUser.getId().equals(user.getId())) {
      return true;
    }
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
    boolean isAdminOrganization = permissions.stream()
      .anyMatch(
        p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION));

    if (isAdminSitmun) {
      return true;
    }

    if (isAdminOrganization) {
      if (user.getId() != null) {
        return this.getUserWithPermissionsByUsername(user.getUsername()).map(u ->
          u.getPermissions().stream()
            .anyMatch(targetDomainObjectPermissions ->
              permissions.stream()
                .filter(p -> p.getRole().getName()
                  .equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION))
                .map(UserConfiguration::getTerritory).map(Territory::getId)
                .collect(Collectors.toList())
                .contains(targetDomainObjectPermissions.getTerritory().getId())
            )
        ).orElse(false);
      } else {
        return true;
      }
    }

    return false;
  }

  public Optional<User> getUserWithPermissionsByUsername(String name) {
    return applicationUserRepository.findOneWithPermissionsByUsername(name);
  }

}
