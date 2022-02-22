package org.sitmun.service;

import org.sitmun.domain.TerritoryType;
import org.sitmun.domain.User;
import org.sitmun.domain.UserConfiguration;
import org.sitmun.security.AuthoritiesConstants;
import org.sitmun.security.PermissionResolver;
import org.sitmun.security.SecurityConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TerritoryTypePermissionResolver implements PermissionResolver<TerritoryType> {

  @Override
  public boolean resolvePermission(User authUser, TerritoryType entity, String permission) {
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));

    if (isAdminSitmun) {
      return true;
    }

    return (permission.equalsIgnoreCase(SecurityConstants.READ_PERMISSION));
  }

}
