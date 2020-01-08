package org.sitmun.plugin.core.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.PermissionResolver;
import org.sitmun.plugin.core.security.SecurityConstants;
import org.springframework.stereotype.Component;

@Component
public class UserConfigurationPermissionResolver implements PermissionResolver<UserConfiguration> {

  @Override
  public boolean resolvePermission(User authUser, UserConfiguration entity, String permission) {
    Set<UserConfiguration> permissions = authUser.getPermissions();
    boolean isAdminSitmun = permissions.stream()
                                .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
    boolean isAdminOrganization = permissions.stream()
                                      .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION));

	  if (isAdminSitmun) {
		  return true;
	  }

    if (permission.equalsIgnoreCase(SecurityConstants.CREATE_PERMISSION)
            || permission.equalsIgnoreCase(SecurityConstants.UPDATE_PERMISSION)
            || permission.equalsIgnoreCase(SecurityConstants.DELETE_PERMISSION)
            || permission.equalsIgnoreCase(SecurityConstants.ADMIN_PERMISSION)) {

      // si tengo el rol de admin de territorio y el permiso es sobre ese territorio
      if (isAdminOrganization) {
        return permissions.stream()
                   .filter(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION))
                   .map(UserConfiguration::getTerritory).map(Territory::getId).collect(Collectors.toList())
                   .contains(entity.getTerritory().getId());
      } else {
        return false;
      }
    } else if (permission.equalsIgnoreCase(SecurityConstants.READ_PERMISSION)) {
      // Si son mis permisos o soy el admin del territorio
      return (authUser.getId().equals(entity.getUser().getId())) || permissions.stream()
                                                                        .filter(p -> p.getRole().getName()
                                                                                         .equalsIgnoreCase(AuthoritiesConstants.ADMIN_ORGANIZACION))
                                                                        .map(UserConfiguration::getTerritory).map(Territory::getId).collect(Collectors.toList())
                                                                        .contains(entity.getTerritory().getId());

    }

    return false;

  }

}
