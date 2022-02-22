package org.sitmun.service;

import org.sitmun.domain.Role;
import org.sitmun.domain.User;
import org.sitmun.domain.UserConfiguration;
import org.sitmun.security.AuthoritiesConstants;
import org.sitmun.security.PermissionResolver;
import org.sitmun.security.SecurityConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RolePermissionResolver implements PermissionResolver<Role> {

  @Override
  public boolean resolvePermission(User authUser, Role entity, String permission) {
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));

    if (isAdminSitmun) {
      return true;
    }

    return (permission.equalsIgnoreCase(SecurityConstants.READ_PERMISSION)
      && !entity.getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION)
      && !entity.getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
  }

}
