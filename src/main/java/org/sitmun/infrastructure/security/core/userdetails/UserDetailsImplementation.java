package org.sitmun.infrastructure.security.core.userdetails;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailsImplementation implements UserDetails {

  @Getter private final Integer id;

  private final String username;

  private final Boolean accountNonLocked;

  private final String password;

  private final Collection<? extends GrantedAuthority> authorities;

  public UserDetailsImplementation(
      Integer id,
      String username,
      String password,
      Collection<? extends GrantedAuthority> authorities,
      Boolean enabled) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.authorities = authorities;
    accountNonLocked = enabled;
  }

  public static UserDetailsImplementation build(User user) {
    List<SecurityRole> roles;
    if (Boolean.TRUE.equals(user.getAdministrator())) {
      roles = Lists.newArrayList(SecurityRole.USER, SecurityRole.ADMIN);
    } else {
      roles = Lists.newArrayList(SecurityRole.USER);
    }
    List<GrantedAuthority> authorities =
        roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.authority()))
            .collect(Collectors.toList());

    return new UserDetailsImplementation(
        user.getId(), user.getUsername(), user.getPassword(), authorities, !user.getBlocked());
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    UserDetailsImplementation user = (UserDetailsImplementation) obj;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
