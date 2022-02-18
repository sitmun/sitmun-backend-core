package org.sitmun.plugin.core.constraints;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {BoundingBoxValidator.class})
@Documented
public @interface BoundingBox {

  /**
   * Required for a Constraint annotation.
   */
  Class<? extends Payload>[] payload() default {};

  /**
   * Required for a Constraint annotation.
   */
  String message() default "Invalid Bounding Box";

  /**
   * Required for a Constraint annotation.
   */
  Class<?>[] groups() default {};

}