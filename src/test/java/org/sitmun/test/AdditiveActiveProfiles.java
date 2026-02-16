package org.sitmun.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.ActiveProfiles;

/**
 * Custom annotation that makes @ActiveProfiles additive.
 *
 * <p>This annotation combines the profiles specified here with the existing spring.profiles.active
 * system property set by Gradle (for database selection: h2, postgres, oracle).
 *
 * <p>Usage: @AdditiveActiveProfiles("ldap") // Adds "ldap" to existing profiles
 * @AdditiveActiveProfiles({"ldap", "mail"}) // Adds both profiles
 *
 * <p>This implementation uses a non-mutating resolver and does not modify JVM-global state,
 * making it parallel-safe.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles(resolver = AdditiveProfilesResolver.class)
public @interface AdditiveActiveProfiles {
  String[] value() default {};
}
