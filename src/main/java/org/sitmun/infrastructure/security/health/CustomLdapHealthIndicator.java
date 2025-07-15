package org.sitmun.infrastructure.security.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom LDAP health indicator that provides detailed health information.
 * Only active when the 'ldap' profile is enabled.
 */
@Component
@Profile("ldap")
@Slf4j
public class CustomLdapHealthIndicator implements HealthIndicator {

    private final LdapTemplate ldapTemplate;

    public CustomLdapHealthIndicator(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    @Override
    public Health health() {
        try {
            // Perform a simple LDAP search to test connectivity
            ldapTemplate.search("", "(objectClass=*)", (Object obj) -> obj);
            
            Map<String, Object> details = new HashMap<>();
            details.put("status", "LDAP connection is healthy");
            details.put("timestamp", System.currentTimeMillis());
            
            return Health.up()
                .withDetails(details)
                .build();
                
        } catch (Exception e) {
            log.error("LDAP health check failed", e);
            
            Map<String, Object> details = new HashMap<>();
            details.put("error", e.getMessage());
            details.put("timestamp", System.currentTimeMillis());
            
            return Health.down()
                .withDetails(details)
                .build();
        }
    }
} 