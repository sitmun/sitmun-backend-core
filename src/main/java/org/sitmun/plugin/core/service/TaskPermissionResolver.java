package org.sitmun.plugin.core.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Component;

@Component
public class TaskPermissionResolver implements PermissionResolver<Task> {

  public boolean resolvePermission(User authUser, Task entity, String permission) {
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
      return (permissions.stream().map(p -> p.getTerritory()).filter(entity.getAvailabilities().stream().map(a -> a.getTerritory()).collect(Collectors.toList())::contains).count() > 0);
    }

    return false;
  }

}
