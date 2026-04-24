package org.sitmun.domain.task;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sitmun.domain.DomainConstants;
import org.sitmun.infrastructure.util.ParameterValidator;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
public class TaskQueryValidator implements TaskValidator {

  @Override
  public boolean accept(Task task) {
    if (task == null || task.getType() == null) {
      return false;
    }
    return Objects.equals("Query", task.getType().getTitle());
  }

  @Override
  public void validate(Task task) throws RepositoryConstraintViolationException {
    Map<String, Object> properties = task.getProperties();
    if (properties == null) {
      return;
    }
    Object scopeObj = properties.get(DomainConstants.Tasks.PROPERTY_SCOPE);
    if (DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY
            .equalsIgnoreCase(String.valueOf(scopeObj))
        && ParameterValidator.hasProvidedVariables(properties)) {
      var errors = init(task);
      errors.rejectValue(
          "properties",
          "parameters.providedNotAllowed",
          "Web API Query (No Proxy) tasks cannot have backend-provided (proxy-injected) variables");
      throw new RepositoryConstraintViolationException(errors);
    }
  }
}
