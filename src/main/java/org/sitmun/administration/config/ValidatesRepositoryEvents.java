package org.sitmun.administration.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring Data REST events that should run this validator. When present, these events replace the
 * bean-name prefix used by {@link AdministrationRestConfigurer}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatesRepositoryEvents {
  String[] value();
}
