package org.sitmun.infrastructure.persistence.type.srs;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.regex.Pattern;

public class SrsSystemListValidator
  implements ConstraintValidator<Srs, List<String>> {

  private static final Pattern pattern = Pattern
    .compile("^[A-Z_\\-]+:\\d+$", Pattern.CASE_INSENSITIVE);

  @Override
  public boolean isValid(List<String> value, ConstraintValidatorContext constraintValidatorContext) {
    if (value == null) {
      return true;
    }
    return value.stream().allMatch(pattern.asMatchPredicate());
  }
}
