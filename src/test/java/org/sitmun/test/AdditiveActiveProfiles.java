package org.sitmun.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Custom annotation that makes @ActiveProfiles additive.
 *
 * <p>This annotation combines the profiles specified here with the existing spring.profiles.active
 * system property set by Gradle.
 *
 * <p>Usage: @AdditiveActiveProfiles(Profiles.LDAP) // Adds ldap to existing
 * profiles; @AdditiveActiveProfiles({Profiles.LDAP, Profiles.MAIL}) // Adds both profiles
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestExecutionListeners(
    listeners = AdditiveProfilesTestExecutionListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public @interface AdditiveActiveProfiles {
  String[] value() default {};
}
