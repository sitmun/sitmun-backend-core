package org.sitmun.test;

import com.google.common.collect.Lists;
import org.sitmun.security.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.stream.Collectors;

public class Fixtures {

  public static User admin() {
    List<Role> roles = Lists.newArrayList(Role.ROLE_ADMIN, Role.ROLE_USER);
    List<GrantedAuthority> authorities = roles.stream()
      .map(role -> new SimpleGrantedAuthority(role.name()))
      .collect(Collectors.toList());
    return new User("admin", "admin", authorities);
  }

}
