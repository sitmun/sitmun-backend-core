package org.sitmun.plugin.core.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class SpatialReferenceSystemValidator
  implements ConstraintValidator<SpatialReferenceSystem, String> {

  private static final Pattern pattern = Pattern.compile("^[A-Z\\-]+:\\d+$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return pattern.matcher(value).matches();
  }
}
