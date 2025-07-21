package org.sitmun.infrastructure.persistence.type.basic;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.validator.constraints.CompositionType.OR;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.hibernate.validator.constraints.ConstraintComposition;
import org.hibernate.validator.constraints.URL;

@ConstraintComposition(OR)
@URL(protocol = "http")
@URL(protocol = "https")
@ReportAsSingleViolation
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
public @interface Http {

  /** Required for a Constraint annotation. */
  String message() default "Invalid value";

  /** Required for a Constraint annotation. */
  Class<?>[] groups() default {};

  /** Required for a Constraint annotation. */
  Class<? extends Payload>[] payload() default {};
}
