package org.sitmun.security.services;

import com.google.common.collect.Lists;
import org.sitmun.domain.User;
import org.sitmun.security.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDetailsImpl implements UserDetails {
  private static final long serialVersionUID = 1L;

  private final Integer id;

  private final String username;

  private final Boolean accountNonLocked;

  private final String password;

  private final Collection<? extends GrantedAuthority> authorities;

  public UserDetailsImpl(Integer id, String username, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         Boolean enabled) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.authorities = authorities;
    this.accountNonLocked = enabled;
  }

  public static UserDetailsImpl build(User user) {
    List<Role> roles;
    if (user.getAdministrator()) {
      roles = Lists.newArrayList(Role.ROLE_USER, Role.ROLE_ADMIN);
    } else {
      roles = Lists.newArrayList(Role.ROLE_USER);
    }
    List<GrantedAuthority> authorities = roles.stream()
      .map(role -> new SimpleGrantedAuthority(role.name()))
      .collect(Collectors.toList());

    return new UserDetailsImpl(
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
    UserDetailsImpl user = (UserDetailsImpl) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}