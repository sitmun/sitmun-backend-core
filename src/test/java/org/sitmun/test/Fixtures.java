package org.sitmun.test;

import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sitmun.domain.role.Role;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class Fixtures {

  public static User admin() {
    List<SecurityRole> roles = Lists.newArrayList(SecurityRole.ADMIN, SecurityRole.USER);
    List<GrantedAuthority> authorities =
        roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.name()))
            .collect(Collectors.toList());
    return new User("admin", "admin", authorities);
  }

  public static Set<Role> createRoles(Integer... roleIds) {
    Set<Role> roles = new HashSet<>();
    for (Integer roleId : roleIds) {
      Role role = Role.builder().id(roleId).name("Role " + roleId).build();
      roles.add(role);
    }
    return roles;
  }
}
