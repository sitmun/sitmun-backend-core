package org.sitmun.domain.task.parameter;

import static org.sitmun.domain.DomainConstants.Tasks.*;
import static org.sitmun.domain.DomainConstants.Tasks.PARAMETERS_DESCRIPTION;
import static org.sitmun.domain.DomainConstants.Tasks.PARAMETERS_FIELD;
import static org.sitmun.domain.DomainConstants.Tasks.PARAMETERS_PROVIDED;
import static org.sitmun.domain.DomainConstants.Tasks.PARAMETERS_REQUIRED;
import static org.sitmun.domain.DomainConstants.Tasks.PARAMETERS_TYPE;
import static org.sitmun.domain.task.parameter.TaskParameterType.*;

import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.task.Task;
import org.sitmun.infrastructure.util.TaskParameterUtil;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Centralized processor for task parameter parsing, classification, and effective-value
 * computation. Consolidates logic previously scattered across ProxyConfigurationService and
 * multiple TaskMapper implementations.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li>Parse task.properties.parameters into typed {@link TaskParameter} records
 *   <li>Classify each parameter into a {@link TaskParameterType}
 *   <li>Filter client-supplied parameters against allowed names (security)
 *   <li>Build effective parameters with strict precedence: locked > provided > client > literal >
 *       empty
 *   <li>Reject client attempts to inject system variable expressions
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskParameterProcessor {

  private final SystemVariableResolver systemVariableResolver;

  /**
   * Broad regex to detect any {@code #{...}} pattern in client-supplied values. Catches arbitrary
   * content inside braces to defend against injection at the client boundary.
   */
  private static final Pattern CLIENT_SYSTEM_VAR_DETECTOR = Pattern.compile("#\\{[^}]*\\}");

  /**
   * Parses task.properties.parameters into TaskParameter records. Returns empty list if task is
   * null or has no parameters property.
   *
   * <p>Parameter name resolution follows: {@code variable > name > label} for backward
   * compatibility.
   *
   * @param task the task whose parameters to parse (may be null)
   * @return list of parsed parameters, empty if none found
   */
  public List<TaskParameter> parse(@Nullable Task task) {
    if (task == null) {
      return Collections.emptyList();
    }

    Map<String, Object> properties = task.getProperties();
    if (properties == null) {
      return Collections.emptyList();
    }

    Object rawParameters = properties.get(PROPERTY_PARAMETERS);
    if (!(rawParameters instanceof List<?> parameterList)) {
      return Collections.emptyList();
    }

    List<TaskParameter> result = new ArrayList<>();
    for (Object rawParam : parameterList) {
      if (!(rawParam instanceof Map<?, ?> param)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> parameter = (Map<String, Object>) param;

      // Resolve name with fallback chain: variable > name > label
      String name = TaskParameterUtil.getParameterVariable(parameter);
      if (!StringUtils.hasText(name)) {
        // Additional fallback to label (used by TaskMoreInfoService)
        Object labelObj = parameter.get(PARAMETERS_LABEL);
        name = labelObj != null ? String.valueOf(labelObj) : null;
      }

      if (!StringUtils.hasText(name)) {
        log.warn("Skipping parameter with no name/variable/label: {}", parameter);
        continue;
      }

      Object rawValueObj = parameter.get(PARAMETERS_VALUE);
      String rawValue = rawValueObj != null ? String.valueOf(rawValueObj) : null;

      Object fieldObj = parameter.get(PARAMETERS_FIELD);
      String field = fieldObj != null ? String.valueOf(fieldObj) : null;

      Object typeObj = parameter.get(PARAMETERS_TYPE);
      String type = typeObj != null ? String.valueOf(typeObj) : null;

      Object requiredObj = parameter.get(PARAMETERS_REQUIRED);
      Boolean required = requiredObj != null ? Boolean.valueOf(String.valueOf(requiredObj)) : null;

      Object providedObj = parameter.get(PARAMETERS_PROVIDED);
      boolean providedFlag =
          Boolean.TRUE.equals(providedObj) || "true".equalsIgnoreCase(String.valueOf(providedObj));

      Object labelObj = parameter.get(PARAMETERS_LABEL);
      String label = labelObj != null ? String.valueOf(labelObj) : null;

      Object descriptionObj = parameter.get(PARAMETERS_DESCRIPTION);
      String description = descriptionObj != null ? String.valueOf(descriptionObj) : null;

      try {
        TaskParameter taskParameter =
            new TaskParameter(
                name, rawValue, field, type, required, providedFlag, label, description, parameter);
        result.add(taskParameter);
      } catch (IllegalArgumentException e) {
        log.error("Failed to create TaskParameter for name='{}': {}", name, e.getMessage());
      }
    }

    return result;
  }

  /**
   * Classifies a parsed parameter into its type based on value content and flags.
   *
   * <p>Classification rules (in precedence order):
   *
   * <ol>
   *   <li>LOCKED: rawValue contains {@code #{...}} (defensive: takes priority over provided flag)
   *   <li>PROVIDED: providedFlag is true
   *   <li>DECLARED_WITH_DEFAULT: non-blank rawValue
   *   <li>DECLARED_WITHOUT_DEFAULT: null or blank rawValue
   * </ol>
   *
   * @param parameter the parameter to classify
   * @return the classification type
   */
  public TaskParameterType classify(TaskParameter parameter) {
    // Defensive: #{...} presence takes priority (regardless of provided flag)
    if (parameter.rawValue() != null
        && SystemVariableResolver.containsSystemVariables(parameter.rawValue())) {
      return LOCKED;
    }

    if (parameter.providedFlag()) {
      return PROVIDED;
    }

    if (StringUtils.hasText(parameter.rawValue())) {
      return DECLARED_WITH_DEFAULT;
    }

    return DECLARED_WITHOUT_DEFAULT;
  }

  /**
   * Returns the set of parameter names that the client is allowed to supply. Excludes LOCKED and
   * PROVIDED parameters.
   *
   * @param parameters the parsed task parameters
   * @return set of client-allowed parameter names
   */
  public Set<String> clientAllowedNames(List<TaskParameter> parameters) {
    Set<String> allowed = new LinkedHashSet<>();
    for (TaskParameter param : parameters) {
      TaskParameterType type = classify(param);
      if (type.isClientAllowed()) {
        allowed.add(param.name());
      }
    }
    return allowed;
  }

  /**
   * Filters client-supplied parameters to keep only declared, client-allowed names. Rejects
   * parameters not declared in the task configuration.
   *
   * <p><b>Security:</b> Only allows parameters explicitly declared in the task and not locked by
   * {@code #{...}} or {@code provided: true}.
   *
   * <p><b>Note:</b> Caller is responsible for stripping pagination keys (limit, offset) before
   * calling this method.
   *
   * @param parameters the parsed task parameters
   * @param clientParameters the client-supplied parameter map (may be null)
   * @return filtered map containing only allowed parameter names
   */
  public Map<String, String> filterClientParameters(
      List<TaskParameter> parameters, @Nullable Map<String, String> clientParameters) {
    if (clientParameters == null || clientParameters.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<String> allowedNames = clientAllowedNames(parameters);

    // Security: filter out any client parameter not in allowedNames
    Map<String, String> filtered = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : clientParameters.entrySet()) {
      if (allowedNames.contains(entry.getKey())) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }

    return filtered;
  }

  /**
   * Builds effective parameters with strict precedence: locked > provided > client > literal >
   * empty.
   *
   * <p>Every declared parameter name appears in the result with a value (possibly empty string).
   *
   * <p>Resolution semantics:
   *
   * <ul>
   *   <li>LOCKED and PROVIDED: resolve {@code #{...}} via SystemVariableResolver; participates even
   *       when resolved to null/blank
   *   <li>DECLARED_WITH_DEFAULT: plain literal participates only when non-blank after resolution
   *   <li>DECLARED_WITHOUT_DEFAULT: empty string if client doesn't supply a value
   * </ul>
   *
   * @param parameters the parsed task parameters
   * @param filteredClientParameters client parameters after filtering (may be null)
   * @param coordinates context for system variable resolution
   * @return effective parameter map with all declared names
   */
  public Map<String, String> buildEffectiveParameters(
      List<TaskParameter> parameters,
      @Nullable Map<String, String> filteredClientParameters,
      RequestCoordinates coordinates) {

    Map<String, String> effective = new LinkedHashMap<>();

    for (TaskParameter param : parameters) {
      TaskParameterType type = classify(param);
      String name = param.name();

      switch (type) {
        case LOCKED:
          // Locked: resolve #{...}, always participates (even if null/blank after resolution)
          String lockedValue =
              systemVariableResolver.resolve(
                  param.rawValue() != null ? param.rawValue() : "", coordinates);
          effective.put(name, lockedValue != null ? lockedValue : "");
          break;

        case PROVIDED:
          // Provided: resolve #{...} if present, always participates
          String providedValue = resolveEffectiveValue(param.rawValue(), coordinates, true);
          effective.put(name, providedValue != null ? providedValue : "");
          break;

        case DECLARED_WITH_DEFAULT:
          // Start with literal default
          String literalDefault = resolveEffectiveValue(param.rawValue(), coordinates, false);
          effective.put(name, Objects.requireNonNullElse(literalDefault, ""));
          // Client can override
          if (filteredClientParameters != null && filteredClientParameters.containsKey(name)) {
            effective.put(name, filteredClientParameters.get(name));
          }
          break;

        case DECLARED_WITHOUT_DEFAULT:
          // Client supplies value, or empty string
          if (filteredClientParameters != null && filteredClientParameters.containsKey(name)) {
            effective.put(name, filteredClientParameters.get(name));
          } else {
            effective.put(name, "");
          }
          break;
      }
    }

    return effective;
  }

  /**
   * Resolves effective backend parameter value. Mirrors the semantics of {@code
   * resolveEffectiveBackendParameterValue} from ProxyConfigurationService.
   *
   * <p>For values containing {@code #{...}}: always participates (even when resolved to
   * null/blank). For plain literals: participates only when non-blank after resolution.
   *
   * @param rawValue the raw parameter value (may contain #{...})
   * @param coordinates context for system variable resolution
   * @param alwaysParticipate if true, returns empty string for null/blank; if false, returns null
   * @return resolved value, or null if should not participate
   */
  private String resolveEffectiveValue(
      @Nullable String rawValue, RequestCoordinates coordinates, boolean alwaysParticipate) {
    if (!StringUtils.hasText(rawValue)) {
      return alwaysParticipate ? "" : null;
    }

    String resolved = systemVariableResolver.resolve(rawValue, coordinates);

    // If raw value contains #{...}, always participate
    if (SystemVariableResolver.containsSystemVariables(rawValue)) {
      return resolved != null ? resolved : "";
    }

    // Plain literal: participate only if non-blank after resolution
    if (!StringUtils.hasText(resolved)) {
      return null;
    }

    return resolved;
  }

  /**
   * Rejects the request if any client-supplied parameter value contains a system variable pattern
   * ({@code #{...}}). This broad check defends against injection attempts with any content inside
   * the braces.
   *
   * @param clientParameters the client-supplied parameters (may be null)
   * @throws BadRequestException if any value contains {@code #{...}}
   */
  public void rejectClientSystemVariables(@Nullable Map<String, String> clientParameters) {
    if (clientParameters == null || clientParameters.isEmpty()) {
      return;
    }

    for (Map.Entry<String, String> entry : clientParameters.entrySet()) {
      String value = entry.getValue();
      if (value != null && CLIENT_SYSTEM_VAR_DETECTOR.matcher(value).find()) {
        throw new BadRequestException(
            "Parameter values containing system variable patterns #{...} are not allowed");
      }
    }
  }

  /**
   * Converts a single parameter to simple DTO format: {@code {type, required}}.
   *
   * <p>Used by TaskQuerySqlService and TaskQueryWebService for client profile DTOs.
   *
   * @param parameter the parameter to convert
   * @param defaultType default type if parameter.type() is null (e.g., "string", "query")
   * @return DTO map with type and required fields
   */
  public Map<String, Object> toSimpleParameterDto(TaskParameter parameter, String defaultType) {
    Map<String, Object> dto = new HashMap<>();
    dto.put(PARAMETERS_TYPE, parameter.type() != null ? parameter.type() : defaultType);
    dto.put(PARAMETERS_REQUIRED, parameter.required() != null && parameter.required());
    return dto;
  }

  /**
   * Converts a single parameter to DTO with optional value: {@code {type, required, value?}}.
   *
   * <p>Used by TaskQueryCartographyService and TaskEditCartographyService for client profile DTOs
   * that include default values.
   *
   * @param parameter the parameter to convert
   * @param defaultType default type if parameter.type() is null
   * @return DTO map with type, required, and optional value fields
   */
  public Map<String, Object> toParameterDtoWithValue(TaskParameter parameter, String defaultType) {
    Map<String, Object> dto = new HashMap<>();
    dto.put(PARAMETERS_TYPE, parameter.type() != null ? parameter.type() : defaultType);
    dto.put(PARAMETERS_REQUIRED, parameter.required() != null && parameter.required());
    if (parameter.rawValue() != null) {
      dto.put(PARAMETERS_VALUE, parameter.rawValue());
    }
    return dto;
  }

  /**
   * Converts a single parameter to Viewer-compatible DTO: {@code {label, value, name, type?,
   * required?}}.
   *
   * <p>Uses field property as value (falls back to rawValue). Conditionally includes type and
   * required fields. Used by TaskMoreInfoService for backward-compatible Viewer DTOs.
   *
   * @param parameter the parameter to convert
   * @return DTO map in Viewer-expected format
   */
  public Map<String, Object> toViewerParameterDto(TaskParameter parameter) {
    Map<String, Object> dto = new HashMap<>();
    dto.put(PARAMETERS_LABEL, parameter.name());
    dto.put(PARAMETERS_VALUE, parameter.field() != null ? parameter.field() : parameter.rawValue());
    dto.put(PARAMETERS_NAME, parameter.name());
    if (parameter.type() != null) {
      dto.put(PARAMETERS_TYPE, parameter.type());
    }
    if (parameter.required() != null) {
      dto.put(PARAMETERS_REQUIRED, parameter.required());
    }
    return dto;
  }
}
