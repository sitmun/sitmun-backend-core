package org.sitmun.plugin.core.service;

import java.util.Set;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Component;

@Component
public class ApplicationBackgroundPermissionResolver implements PermissionResolver<ApplicationBackground> {

  public boolean resolvePermission(User authUser, ApplicationBackground entity, String permission) {
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
      return (permissions.stream().map(p -> p.getRole()).filter(entity.getApplication().getAvailableRoles()::contains).count() > 0)
                 && (permissions.stream().map(p -> p.getRole()).filter(entity.getBackground().getCartographyGroup().getRoles()::contains).count() > 0)
          ;
    }

    return false;
  }
}
