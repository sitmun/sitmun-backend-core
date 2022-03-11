package org.sitmun.common.types.basic;

import org.hibernate.validator.constraints.ConstraintComposition;
import org.hibernate.validator.constraints.URL;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.validator.constraints.CompositionType.OR;

@ConstraintComposition(OR)
@URL(protocol = "http")
@URL(protocol = "https")
@ReportAsSingleViolation
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface Http {

  /**
   * Required for a Constraint annotation.
   */
  String message() default "Invalid value";

  /**
   * Required for a Constraint annotation.
   */
  Class<?>[] groups() default {};

  /**
   * Required for a Constraint annotation.
   */
  Class<? extends Payload>[] payload() default {};
}
