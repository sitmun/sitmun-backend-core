package org.sitmun.plugin.core.service;

import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TerritoryPermissionResolver implements PermissionResolver<Territory> {

  @Override
  public boolean resolvePermission(User authUser, Territory entity, String permission) {
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
    if (isAdminSitmun) {
      return true;
    }
    return (permission.equalsIgnoreCase(SecurityConstants.READ_PERMISSION));

  }

}
