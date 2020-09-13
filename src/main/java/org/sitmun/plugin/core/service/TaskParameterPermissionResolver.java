package org.sitmun.plugin.core.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.sitmun.plugin.core.domain.TaskAvailability;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Component;

@Component
public class TaskParameterPermissionResolver implements PermissionResolver<TaskParameter> {

  public boolean resolvePermission(User authUser, TaskParameter entity, String permission) {
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
      return (permissions.stream().map(UserConfiguration::getTerritory).anyMatch(
        entity.getTask().getAvailabilities().stream().map(TaskAvailability::getTerritory)
          .collect(Collectors.toList())::contains));
    }

    return false;
  }

}
