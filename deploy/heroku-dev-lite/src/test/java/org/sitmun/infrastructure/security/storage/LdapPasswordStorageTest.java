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

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.Schema;

@Profile("test")
@Component
@Order(1)
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

	@Value("${spring.ldap.embedded.ldif}")
	private String ldif;

	@Value("${spring.ldap.embedded.schema.ldif}")
	private String schemaLdif;

	public LdapPasswordStorageTest(LdapUserAuthoritiesPopulator ldapUserAuthoritiesPopulator) {
		this.ldapUserAuthoritiesPopulator = ldapUserAuthoritiesPopulator;
	}

	@Override
	public void addPasswordStorage(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
		createLdapServer();
		String url = StringUtils.hasText(this.host) && StringUtils.hasText(this.baseDN)
				? this.host.concat("/").concat(this.baseDN)
				: null;
		authenticationManagerBuilder.ldapAuthentication()
				.passwordEncoder(ldapPasswordEncoder())
				.userDnPatterns(this.userDNPattern)
				.ldapAuthoritiesPopulator(ldapUserAuthoritiesPopulator)
				.contextSource().url(url)
				.root(this.baseDN);
	}

	private void createLdapServer() {
		InMemoryDirectoryServer directoryServer = null;
		try {
			InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(this.baseDN);
			config.setSchema(Schema.getDefaultStandardSchema());
			directoryServer = new InMemoryDirectoryServer(config);

			// Iniciar el servidor LDAP embebido
			directoryServer.startListening();
			LDAPConnection conn = directoryServer.getConnection();
			this.host = "ldap://" + conn.getHostPort();
			conn.close();

			// Agregar definiciones de nuevos atributos
			directoryServer.applyChangesFromLDIF(this.schemaLdif);
			directoryServer.importFromLDIF(false, this.ldif);
		} catch (LDAPException e) {
			e.printStackTrace();
		}
	}

	public PasswordEncoder ldapPasswordEncoder() {
		return new LdapShaPasswordEncoder();
	}
}