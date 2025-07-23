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
  PROXY;

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
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(ADMIN.authority()));
  }

  public static boolean isUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(USER.authority()));
  }

  public static boolean isPublic() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(PUBLIC.authority()));
  }

  public static boolean isProxy() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(PROXY.authority()));
  }
}
