package org.sitmun.domain.cartography.style;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class BeforeCreateCartographyStylesValidator implements Validator {

  private final CartographyStyleRepository repository;

  public BeforeCreateCartographyStylesValidator(CartographyStyleRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean supports(@NonNull Class<?> aClass) {
    return CartographyStyle.class.equals(aClass);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    CartographyStyle style = (CartographyStyle) target;
    if (Boolean.TRUE.equals(style.getDefaultStyle())
        && repository.countDefaultStyles(style.getCartography()) > 0) {
      errors.rejectValue(
          "defaultStyle",
          "cartography.styles.invalid",
          "Already a default style exists for the cartography.");
    }
  }
}
