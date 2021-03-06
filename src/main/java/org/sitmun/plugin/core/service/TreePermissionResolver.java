package org.sitmun.plugin.core.service;

import org.sitmun.plugin.core.domain.Tree;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TreePermissionResolver implements PermissionResolver<Tree> {

  public boolean resolvePermission(User authUser, Tree entity, String permission) {
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
    if (isAdminSitmun) {
      return true;
    }
    if (permission.equalsIgnoreCase(SecurityConstants.CREATE_PERMISSION)
      || permission.equalsIgnoreCase(SecurityConstants.UPDATE_PERMISSION)
      || permission.equalsIgnoreCase(SecurityConstants.DELETE_PERMISSION)
      || permission.equalsIgnoreCase(SecurityConstants.ADMIN_PERMISSION)) {
      return false;
    } else if (permission.equalsIgnoreCase(SecurityConstants.READ_PERMISSION)) {
      return (permissions.stream().map(UserConfiguration::getRole)
        .anyMatch(entity.getAvailableRoles()::contains));
    }

    return false;
  }

}
