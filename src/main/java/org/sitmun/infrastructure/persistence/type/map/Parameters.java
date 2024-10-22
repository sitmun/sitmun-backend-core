package org.sitmun.infrastructure.persistence.type.map;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@Target(FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ParametersValidator.class)
public @interface Parameters {

  /**
   * Required for a Constraint annotation.
   */
  Class<? extends Payload>[] payload() default {};

  /**
   * Required for a Constraint annotation.
   */
  String message() default "Invalid Parameter";

  /**
   * Required for a Constraint annotation.
   */
  Class<?>[] groups() default {};

}
