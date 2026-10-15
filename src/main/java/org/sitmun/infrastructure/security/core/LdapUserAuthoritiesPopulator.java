package org.sitmun.infrastructure.security.core;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.sitmun.infrastructure.config.Profiles;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.stereotype.Component;

@Profile(Profiles.LDAP)
@RequiredArgsConstructor
@Component
public class LdapUserAuthoritiesPopulator implements LdapAuthoritiesPopulator {

  private final UserDetailsService userDetailsService;

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(
      DirContextOperations dirContextOperations, String username) {
    try {
      return userDetailsService.loadUserByUsername(username).getAuthorities();
    } catch (UsernameNotFoundException ue) {
      throw ue;
    } catch (Exception e) {
      throw new InternalAuthenticationServiceException(
          "Exception occurred while trying to fetch the user authorities from the database", e);
    }
  }
}
