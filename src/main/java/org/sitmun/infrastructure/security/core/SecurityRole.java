package org.sitmun.infrastructure.security.core;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public enum SecurityRole {
  /** An authenticated user with basic permissions. */
  USER,
  /** An authenticated user with administrative permissions. ADMIN implies USER. */
  ADMIN,
  /** A user with public access without authentication. */
  PUBLIC,
  /** A user with proxy permissions. */
  PROXY,
  /** Mobile edition client authenticated with a scoped Bearer access token. */
  MOBILE_EDITION;

  public String authority() {
    return "ROLE_" + name();
  }

  public static List<GrantedAuthority> createAuthorityList(SecurityRole... roles) {
    List<GrantedAuthority> grantedAuthorities = new ArrayList<>(roles.length);
    for (SecurityRole role : roles) {
      grantedAuthorities.add(new SimpleGrantedAuthority(role.authority()));
    }
    return grantedAuthorities;
  }

  public static boolean isAdmin() {
    return hasAuthority(ADMIN);
  }

  public static boolean isUser() {
    return hasAuthority(USER);
  }

  public static boolean isPublic() {
    return hasAuthority(PUBLIC);
  }

  public static boolean isProxy() {
    return hasAuthority(PROXY);
  }

  public static boolean isMobileEdition() {
    return hasAuthority(MOBILE_EDITION);
  }

  private static boolean hasAuthority(SecurityRole role) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role.authority()));
  }
}
