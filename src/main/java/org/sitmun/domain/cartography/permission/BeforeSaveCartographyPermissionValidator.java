package org.sitmun.domain.cartography.permission;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class BeforeSaveCartographyPermissionValidator implements Validator {
  @Override
  public boolean supports(@NonNull Class<?> aClass) {
    return CartographyPermission.class.equals(aClass);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    CartographyPermission cartographyPermission = (CartographyPermission) target;
    if (!cartographyPermission.getApplications().isEmpty()
        && !cartographyPermission.getType().equals(CartographyPermission.TYPE_SITUATION_MAP)) {
      errors.rejectValue("type", "type.invalid", "In use in an application, can't be modified");
    }
    if (!cartographyPermission.getBackgrounds().isEmpty()
        && !cartographyPermission.getType().equals(CartographyPermission.TYPE_BACKGROUND_MAP)) {
      errors.rejectValue("type", "type.invalid", "In use in a background, can't be modified");
    }
  }
}
