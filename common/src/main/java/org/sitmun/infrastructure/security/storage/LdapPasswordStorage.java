package org.sitmun.infrastructure.security.storage;

import org.sitmun.infrastructure.security.core.LdapUserAuthoritiesPopulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Profile("ldap")
@Component
@Order(1)
public class LdapPasswordStorage implements PasswordStorage {

  private final LdapUserAuthoritiesPopulator ldapUserAuthoritiesPopulator;

  @Value("${sitmun.authentication.ldap.url}")
  private String host;

  @Value("${sitmun.authentication.ldap.base-dn}")
  private String baseDN;

  @Value("${sitmun.authentication.ldap.user-dn-pattern}")
  private String userDNPattern;

  @Value("${sitmum.authentication.ldap.username:}")
  private String userLdap;

  @Value("${sitmun.authentication.ldap.password:}")
  private String passwordLdap;

  public LdapPasswordStorage(LdapUserAuthoritiesPopulator ldapUserAuthoritiesPopulator) {
    this.ldapUserAuthoritiesPopulator = ldapUserAuthoritiesPopulator;
  }

  @Override
  public void addPasswordStorage(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
    String url = StringUtils.hasText(host) && StringUtils.hasText(baseDN)
      ? this.host + "/" + this.baseDN
      : null;
    authenticationManagerBuilder.ldapAuthentication()
      .passwordEncoder(ldapPasswordEncoder())
      .userDnPatterns(userDNPattern)
      .ldapAuthoritiesPopulator(ldapUserAuthoritiesPopulator)
      .contextSource().url(url)
      .managerDn(StringUtils.hasText(userLdap) ? userLdap : null)
      .managerPassword(StringUtils.hasText(passwordLdap) ? passwordLdap : null);
  }

  public PasswordEncoder ldapPasswordEncoder() {
    //noinspection deprecation
    return new LdapShaPasswordEncoder();
  }
}