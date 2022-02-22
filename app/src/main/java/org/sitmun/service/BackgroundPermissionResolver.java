package org.sitmun.service;

import org.sitmun.domain.Background;
import org.sitmun.domain.User;
import org.sitmun.domain.UserConfiguration;
import org.sitmun.security.AuthoritiesConstants;
import org.sitmun.security.PermissionResolver;
import org.sitmun.security.SecurityConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BackgroundPermissionResolver implements PermissionResolver<Background> {

  public boolean resolvePermission(User authUser, Background entity, String permission) {
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

      if (entity.getCartographyGroup() != null) {
        return (permissions.stream().map(UserConfiguration::getRole).anyMatch(entity.getCartographyGroup().getRoles()::contains));
      } else {
        return false;
      }
    }

    return false;
  }

}
