package org.sitmun.infrastructure.persistence.type.boundingbox;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;

public class BoundingBoxValidator implements ConstraintValidator<BoundingBox, Envelope> {

  @Override
  public boolean isValid(Envelope value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null) {
      return true;
    }
    constraintValidatorContext.disableDefaultConstraintViolation();

    boolean result = true;
    if (value.getMaxX() == null) {
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("maxX must not be null")
          .addConstraintViolation();
      result = false;
    }
    if (value.getMaxY() == null) {
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("maxY must not be null")
          .addConstraintViolation();
      result = false;
    }
    if (value.getMinX() == null) {
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("minX must not be null")
          .addConstraintViolation();
      result = false;
    }
    if (value.getMinY() == null) {
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("minY must not be null")
          .addConstraintViolation();
      result = false;
    }
    if (value.getMinX() != null && value.getMaxX() != null && value.getMaxX() <= value.getMinX()) {
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("maxX must be greater than minX")
          .addConstraintViolation();
      result = false;
    }
    if (value.getMinY() != null && value.getMaxY() != null && value.getMaxY() <= value.getMinY()) {
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("maxY must be greater than minY")
          .addConstraintViolation();
      result = false;
    }
    return result;
  }
}
