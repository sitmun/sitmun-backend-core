package org.sitmun.plugin.core.constraints;

import org.sitmun.plugin.core.domain.Envelope;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class BoundingBoxValidator
  implements ConstraintValidator<BoundingBox, Envelope> {

  @Override
  public boolean isValid(Envelope value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    boolean result = true;
    context.disableDefaultConstraintViolation();

    if (value.getMaxX() == null) {
      context.buildConstraintViolationWithTemplate("maxX must not be null")
        .addConstraintViolation();
      result = false;
    }
    if (value.getMaxY() == null) {
      context.buildConstraintViolationWithTemplate("maxY must not be null")
        .addConstraintViolation();
      result = false;
    }
    if (value.getMinX() == null) {
      context.buildConstraintViolationWithTemplate("minX must not be null")
        .addConstraintViolation();
      result = false;
    }
    if (value.getMinY() == null) {
      context.buildConstraintViolationWithTemplate("minY must not be null")
        .addConstraintViolation();
      result = false;
    }
    if (value.getMinX() != null && value.getMaxX() != null && value.getMaxX() <= value.getMinX()) {
      context.buildConstraintViolationWithTemplate("maxX must be greater than minX")
        .addConstraintViolation();
      result = false;
    }
    if (value.getMinY() != null && value.getMaxY() != null && value.getMaxY() <= value.getMinY()) {
      context.buildConstraintViolationWithTemplate("maxY must be greater than minY")
        .addConstraintViolation();
      result = false;
    }
    return result;
  }
}
