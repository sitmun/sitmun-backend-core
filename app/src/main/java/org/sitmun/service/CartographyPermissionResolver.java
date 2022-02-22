package org.sitmun.service;

import org.sitmun.domain.*;
import org.sitmun.security.AuthoritiesConstants;
import org.sitmun.security.PermissionResolver;
import org.sitmun.security.SecurityConstants;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CartographyPermissionResolver implements PermissionResolver<Cartography> {

  public boolean resolvePermission(User authUser, Cartography entity, String permission) {
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
      List<Territory> availableTerritories = entity.getAvailabilities().stream()
        .map(CartographyAvailability::getTerritory)
        .collect(Collectors.toList());
      return permissions.stream()
        .map(UserConfiguration::getTerritory)
        .anyMatch(availableTerritories::contains);
    }

    return false;
  }

}
