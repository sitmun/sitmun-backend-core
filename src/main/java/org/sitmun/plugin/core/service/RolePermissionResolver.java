package org.sitmun.plugin.core.service;

import java.util.Set;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Component;

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

    return (permission.equalsIgnoreCase(SecurityConstants.READ_PERMISSION) && !entity.getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION) && !entity.getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
  }

}
