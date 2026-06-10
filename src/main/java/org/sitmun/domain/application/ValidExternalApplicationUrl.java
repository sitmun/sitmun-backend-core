package org.sitmun.domain.application;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Requires a non-blank http(s) URL in {@code jspTemplate} when {@code type} is external. */
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ExternalApplicationUrlValidator.class)
public @interface ValidExternalApplicationUrl {

  String message() default "entity.application.error.externalUrlRequired";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
