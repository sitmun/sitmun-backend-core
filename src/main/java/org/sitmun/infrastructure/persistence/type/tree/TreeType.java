package org.sitmun.infrastructure.persistence.type.tree;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;;

@Constraint(validatedBy = TreeValidator.class)
@Target(TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TreeType {

  /**
   * Required for a Constraint annotation.
   */
  String message() default "Invalid application asignation";

  /**
   * Required for a Constraint annotation.
   */
  Class<?>[] groups() default {};

  /**
   * Required for a Constraint annotation.
   */
  Class<? extends Payload>[] payload() default {};
}
