package org.sitmun.domain.task;

import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sitmun.authorization.client.service.support.BasicParameterValueConverter;
import org.sitmun.authorization.client.service.support.BasicParameterValueType;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@RequiredArgsConstructor
public class TaskBasicValidator implements TaskValidator {

  /** Spring-binding field for {@link Task#getProperties()} constraint targets. */
  private static final String ERRORS_PROPERTIES_FIELD = "properties";

  private final BasicParameterValueConverter parameterValueConverter;

  @Override
  public boolean accept(Task task) {
    return isLegacyBasicTask(task);
  }

  @Override
  public void validate(Task task) throws RepositoryConstraintViolationException {
    List<Map<String, Object>> parameters = getParameters(task);
    Errors errors = init(task);
    for (Map<String, Object> parameter : parameters) {
      validateLegacyBasicParameter(parameter, errors);
    }
    failIfErrored(errors);
  }

  private void failIfErrored(Errors errors) throws RepositoryConstraintViolationException {
    if (errors.hasErrors()) {
      throw new RepositoryConstraintViolationException(errors);
    }
  }

  private void validateLegacyBasicParameter(Map<String, Object> parameter, Errors errors) {
    if (!hasNameTypeValueKeys(parameter)) {
      errors.rejectValue(
          ERRORS_PROPERTIES_FIELD,
          "parameters.missing",
          "[name], [type] and [value] properties were expected");
      return;
    }
    if (parameter.size() != 3) {
      errors.rejectValue(
          ERRORS_PROPERTIES_FIELD, "parameters.extra", "Extra properties were found");
      return;
    }
    if (!(parameter.get(PARAMETERS_NAME) instanceof String name)
        || !(parameter.get(PARAMETERS_TYPE) instanceof String type)) {
      errors.rejectValue(ERRORS_PROPERTIES_FIELD, "parameters.invalid", "Invalid parameter types");
      return;
    }
    validateValueMatchesDeclaredType(name, type, parameter.get(PARAMETERS_VALUE), errors);
  }

  private static boolean hasNameTypeValueKeys(Map<String, Object> parameter) {
    return parameter.containsKey(PARAMETERS_NAME)
        && parameter.containsKey(PARAMETERS_TYPE)
        && parameter.containsKey(PARAMETERS_VALUE);
  }

  private void validateValueMatchesDeclaredType(
      String name, String typeString, Object value, Errors errors) {
    BasicParameterValueType type = BasicParameterValueType.from(typeString);
    if (type == null) {
      errors.rejectValue(
          ERRORS_PROPERTIES_FIELD,
          "parameters.any",
          name + " property contains '" + value + "' when type '" + typeString + "'");
      return;
    }
    parameterValueConverter.validate(name, type, value, errors);
  }
}
