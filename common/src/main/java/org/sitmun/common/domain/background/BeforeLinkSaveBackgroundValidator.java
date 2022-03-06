package org.sitmun.common.domain.background;

import org.sitmun.common.domain.cartography.permission.CartographyPermission;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;

@Component
public class BeforeLinkSaveBackgroundValidator implements Validator {
  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return Background.class.equals(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    Background background = (Background) target;
    if (background.getCartographyGroup() != null && !Objects.equals(background.getCartographyGroup().getType(), CartographyPermission.TYPE_BACKGROUND_MAP)) {
      errors.rejectValue("cartographyGroup.type", "cartographyGroup.type.invalid", "It must be of type \"" + CartographyPermission.TYPE_BACKGROUND_MAP + "\".");
    }
  }
}
