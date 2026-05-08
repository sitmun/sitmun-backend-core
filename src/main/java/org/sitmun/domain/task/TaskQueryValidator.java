package org.sitmun.domain.task;

import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.infrastructure.util.ParameterValidator;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskQueryValidator implements TaskValidator {

  private final TaskParameterProcessor taskParameterProcessor;

  private static final String ERRORS_PROPERTIES_FIELD = "properties";
  private static final String ERROR_CODE_DIRECT_EXECUTION =
      "directExecution.backendFeaturesNotAllowed";

  private static final String DIRECT_EXECUTION_VIOLATION_MESSAGE_FORMAT =
      "Direct execution tasks (%s) cannot use backend-only features: %s";

  /**
   * Applies only when {@link org.sitmun.domain.DomainConstants.Tasks#isQueryTask(Task)} is true.
   */
  @Override
  public boolean accept(Task task) {
    return isQueryTask(task);
  }

  @Override
  public void validate(Task task) throws RepositoryConstraintViolationException {
    if (task == null) {
      return;
    }
    Map<String, Object> properties = task.getProperties();
    if (properties == null) {
      return;
    }
    Object scopeObj = properties.get(PROPERTY_SCOPE);
    String scope = String.valueOf(scopeObj);

    // Strict validation for direct execution scopes (no-proxy and external-link)
    if (SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scope)
        || SCOPE_URL_QUERY.equalsIgnoreCase(scope)) {
      validateDirectExecutionScope(task, properties, scope);
    }
  }

  /**
   * Validates that direct execution tasks (no-proxy and external-link) do not contain any
   * backend-only features that require proxy execution.
   */
  private void validateDirectExecutionScope(
      Task task, Map<String, Object> properties, String scope) {
    List<String> violations = new ArrayList<>();

    // Parse parameters using TaskParameterProcessor
    List<TaskParameter> parameters = taskParameterProcessor.parse(task);

    // Check for provided parameters
    if (ParameterValidator.hasProvidedVariables(parameters)) {
      violations.add("Tasks with direct execution cannot have provided parameters");
    }

    // Check for system variables in command
    Object commandObj = properties.get(PROPERTY_COMMAND);
    if (commandObj != null
        && ParameterValidator.containsSystemVariables(String.valueOf(commandObj))) {
      violations.add("Command URL cannot contain system variables #{...}");
    }

    // Check for system variables in parameter values
    if (ParameterValidator.containsSystemVariablesInParameters(parameters)) {
      violations.add("Parameter values cannot contain system variables #{...}");
    }

    // Check for system variables in headers
    @SuppressWarnings("unchecked")
    Map<String, Object> headers =
        (Map<String, Object>) properties.getOrDefault(PROPERTY_HEADERS, Collections.emptyMap());
    if (!headers.isEmpty()) {
      violations.add("Headers are not supported (requires proxy execution)");
      if (ParameterValidator.containsSystemVariablesInMapValues(headers)) {
        violations.add("Header values cannot contain system variables #{...}");
      }
    }

    // Check for system variables in queryParams
    @SuppressWarnings("unchecked")
    Map<String, Object> queryParams =
        (Map<String, Object>)
            properties.getOrDefault(PROPERTY_QUERY_PARAMS, Collections.emptyMap());
    if (!queryParams.isEmpty()) {
      violations.add("Query parameters are not supported (requires proxy execution)");
      if (ParameterValidator.containsSystemVariablesInMapValues(queryParams)) {
        violations.add("Query parameter values cannot contain system variables #{...}");
      }
    }

    // Check for authentication mode (must be null or "None")
    Object authModeObj = properties.get(PROPERTY_AUTHENTICATION_MODE);
    if (authModeObj != null
        && !AUTHENTICATION_MODE_NONE.equalsIgnoreCase(String.valueOf(authModeObj))
        && !String.valueOf(authModeObj).trim().isEmpty()) {
      violations.add("Authentication is not supported (requires proxy execution)");
    }

    // If there are any violations, throw exception
    if (!violations.isEmpty()) {
      var errors = init(task);
      String message =
          String.format(
              DIRECT_EXECUTION_VIOLATION_MESSAGE_FORMAT, scope, String.join("; ", violations));
      errors.rejectValue(ERRORS_PROPERTIES_FIELD, ERROR_CODE_DIRECT_EXECUTION, message);
      throw new RepositoryConstraintViolationException(errors);
    }
  }
}
