package org.sitmun.plugin.core.validators;

import org.sitmun.plugin.core.domain.Application;
import org.sitmun.plugin.core.domain.CartographyPermission;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Objects;

@Component
public class BeforeCreateApplicationValidator implements Validator {
  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return Application.class.equals(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    Application application = (Application) target;
    if (application.getSituationMap() != null && !Objects.equals(application.getSituationMap().getType(), CartographyPermission.TYPE_SITUATION_MAP)) {
      errors.rejectValue("situationMap.type", "situationMap.type.invalid", "It must be of type \"" + CartographyPermission.TYPE_SITUATION_MAP + "\".");
    }
  }
}
