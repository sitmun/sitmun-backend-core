package org.sitmun.domain.cartography;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class BeforeLinkSaveCartographyValidator implements Validator {
  @Override
  public boolean supports(@NonNull Class<?> aClass) {
    return Cartography.class.equals(aClass);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    Cartography cartography = (Cartography) target;
    if (cartography.getStyles() != null &&
      cartography.getStyles().stream()
        .map(style -> Boolean.TRUE.equals(style.getDefaultStyle()) ? 1 : 0)
        .reduce(0, Integer::sum) > 1) {
      errors.rejectValue("styles", "cartography.styles.invalid", "Multiple default styles for this cartography.");
    }
  }
}
