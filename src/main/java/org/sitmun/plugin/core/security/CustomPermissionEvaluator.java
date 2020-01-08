package org.sitmun.plugin.core.security;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.sitmun.plugin.core.domain.User;
import org.sitmun.plugin.core.domain.UserConfiguration;
import org.sitmun.plugin.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.ResolvableType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionEvaluator")
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CustomPermissionEvaluator implements PermissionEvaluator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CustomPermissionEvaluator.class);

  @Autowired
  private UserService userService;

  @Autowired
  private ApplicationContext context;

  @PersistenceContext
  private EntityManager em;

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {

    if ((authentication == null) || (targetDomainObject == null) || !(permission instanceof String)) {
      return false;
    }

    Optional<User> currentUser = null;
    if ((authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User)) {
      currentUser = this.userService.getUserWithPermissionsByUsername(
          ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername());

    } else {
      currentUser = this.userService.getUserWithPermissionsByUsername(authentication.getPrincipal().toString());

    }


    if (currentUser.isPresent()) {
      User user = currentUser.get();
      try {
        String[] beanNamesForType = context.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(PermissionResolver.class, targetDomainObject.getClass()));
        if (beanNamesForType.length > 0) {
          PermissionResolver rc;
          rc = (PermissionResolver) context.getBean(beanNamesForType[0]);
          if (rc != null) {
            return rc.resolvePermission(user, targetDomainObject, (String) permission);
          }
        }
      } catch (BeansException e) {
        LOGGER.error("Can't resolve bean for class " + targetDomainObject.getClass(), e);
      }
      //return true;
      Set<UserConfiguration> permissions = user.getPermissions();
      boolean isAdminSitmun = permissions.stream()
                                  .anyMatch(p -> p.getRole().getName().equalsIgnoreCase(AuthoritiesConstants.ADMIN_SITMUN));
      if (isAdminSitmun) {
        return true;
      }
      return (((String) permission).equalsIgnoreCase(SecurityConstants.READ_PERMISSION));
    } else {
      return false;
    }

  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                               Object permission) {
    if ((authentication == null) || (targetType == null) || !(permission instanceof String)) {
      return false;
    }

    Optional<User> currentUser = this.userService.getUserWithPermissionsByUsername(authentication.getName());

    LOGGER.info("Checking permission of {}", authentication.getName());

    if (currentUser.isPresent()) {
      User user = currentUser.get();

      try {
        String[] beanNamesForType = context.getBeanNamesForType(
            ResolvableType.forClassWithGenerics(PermissionResolver.class, Class.forName(targetType)));
        if (beanNamesForType.length > 0) {
          PermissionResolver rc;
          rc = (PermissionResolver) context.getBean(beanNamesForType[0]);
          if (rc != null) {
            return rc.resolvePermission(user, em.find(Class.forName(targetType), targetId), (String) permission);
          }
        }
      } catch (BeansException e) {
        LOGGER.error("Can't resolve bean for class " + targetType, e);
      } catch (ClassNotFoundException e) {
        LOGGER.error(e.getMessage(), e);
      }

      return true;
    } else {
      LOGGER.info("{} not found", authentication.getName());
      return false;
    }

  }

}
