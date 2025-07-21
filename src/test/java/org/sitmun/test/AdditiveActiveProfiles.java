package org.sitmun.test;

import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation that makes @ActiveProfiles additive.
 * 
 * This annotation combines the profiles specified here with the existing
 * spring.profiles.active system property set by Gradle.
 * 
 * Usage:
 * @AdditiveActiveProfiles("ldap")  // Adds "ldap" to existing profiles
 * @AdditiveActiveProfiles({"ldap", "mail"})  // Adds both profiles
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestExecutionListeners(
    listeners = AdditiveProfilesTestExecutionListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public @interface AdditiveActiveProfiles {
    String[] value() default {};
} 