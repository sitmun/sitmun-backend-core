package org.sitmun.infrastructure.security.core.userdetails;

import com.google.common.collect.Lists;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDetailsImplementation implements UserDetails {
  private static final long serialVersionUID = 1L;

  private final Integer id;

  private final String username;

  private final Boolean accountNonLocked;

  private final String password;

  private final Collection<? extends GrantedAuthority> authorities;

  public UserDetailsImplementation(Integer id, String username, String password,
                                   Collection<? extends GrantedAuthority> authorities,
                                   Boolean enabled) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.authorities = authorities;
    this.accountNonLocked = enabled;
  }

  public static UserDetailsImplementation build(User user) {
    List<SecurityRole> roles;
    if (Boolean.TRUE.equals(user.getAdministrator())) {
      roles = Lists.newArrayList(SecurityRole.ROLE_USER, SecurityRole.ROLE_ADMIN);
    } else {
      roles = Lists.newArrayList(SecurityRole.ROLE_USER);
    }
    List<GrantedAuthority> authorities = roles.stream()
      .map(role -> new SimpleGrantedAuthority(role.name()))
      .collect(Collectors.toList());

    return new UserDetailsImplementation(
      user.getId(),
      user.getUsername(),
      user.getPassword(),
      authorities,
      !user.getBlocked()
    );
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  public Integer getId() {
    return id;
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
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    UserDetailsImplementation user = (UserDetailsImplementation) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}