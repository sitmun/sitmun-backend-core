package org.sitmun.plugin.core.constraints;

import java.util.List;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SpatialReferenceSystemListValidator
    implements ConstraintValidator<SpatialReferenceSystem, List<String>> {

  private static final Pattern pattern = Pattern
      .compile("^[A-Z_\\-]+:\\d+$", Pattern.CASE_INSENSITIVE);

  @Override
  public void initialize(SpatialReferenceSystem constraintAnnotation) {
  }

  @Override
  public boolean isValid(List<String> value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.stream().allMatch(it -> pattern.matcher(it).matches());
  }
}
