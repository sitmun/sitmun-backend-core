package org.sitmun.constraints;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.regex.Pattern;

public class SpatialReferenceSystemListValidator
  implements ConstraintValidator<SpatialReferenceSystem, List<String>> {

  private static final Pattern pattern = Pattern
    .compile("^[A-Z_\\-]+:\\d+$", Pattern.CASE_INSENSITIVE);

  @Override
  public boolean isValid(List<String> value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return value.stream().allMatch(it -> pattern.matcher(it).matches());
  }
}
