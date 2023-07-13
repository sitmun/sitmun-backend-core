package org.sitmun.infrastructure.security.storage;

import org.sitmun.infrastructure.security.core.LdapUserAuthoritiesPopulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Profile("test")
@Component
@Order(1)
@Slf4j
public class LdapPasswordStorageTest implements PasswordStorage {

	private final LdapUserAuthoritiesPopulator ldapUserAuthoritiesPopulator;

	@Value("${security.authentication.ldap.host}")
	private String host;

	@Value("${security.authentication.ldap.base_dn}")
	private String baseDN;

	@Value("${security.authentication.ldap.user_dn_pattern}")
	private String userDNPattern;

	@Value("${security.authentication.ldap.user_ldap}")
	private String userLdap;

	@Value("${security.authentication.ldap.password_ldap}")
	private String passwordLdap;

	public LdapPasswordStorageTest(LdapUserAuthoritiesPopulator ldapUserAuthoritiesPopulator) {
		this.ldapUserAuthoritiesPopulator = ldapUserAuthoritiesPopulator;
	}

	@Override
	public void addPasswordStorage(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
		authenticationManagerBuilder.ldapAuthentication()
				.passwordEncoder(ldapPasswordEncoder())
				.userDnPatterns(this.userDNPattern)
				.ldapAuthoritiesPopulator(ldapUserAuthoritiesPopulator);
	}

	public PasswordEncoder ldapPasswordEncoder() {
		return new LdapShaPasswordEncoder();
	}
}