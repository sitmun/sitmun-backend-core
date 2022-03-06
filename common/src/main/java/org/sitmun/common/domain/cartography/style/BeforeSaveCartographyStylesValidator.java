package org.sitmun.common.domain.cartography.style;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class BeforeSaveCartographyStylesValidator implements Validator {

  private final CartographyStyleRepository repository;

  public BeforeSaveCartographyStylesValidator(CartographyStyleRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return CartographyStyle.class.equals(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    CartographyStyle style = (CartographyStyle) target;
    if (style.getDefaultStyle() && repository.countDefaultStylesButThis(style.getCartography(), style) > 0) {
      errors.rejectValue("styles", "cartography.styles.invalid", "Already a default style exists for the cartography.");
    }
  }
}
