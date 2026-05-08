package org.sitmun.domain.task.parameter;

import static org.sitmun.domain.DomainConstants.Tasks.*;
import static org.sitmun.domain.task.parameter.TaskParameterType.*;

import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.sitmun.authorization.client.dto.profile.FeatureInfoParameter;
import org.sitmun.authorization.client.dto.profile.QueryParameter;
import org.sitmun.authorization.client.dto.profile.ServiceParameter;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.task.Task;
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
  private static final Pattern CLIENT_SYSTEM_VAR_DETECTOR = Pattern.compile("#\\{[^}]*}");

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
      String name = null;

      // Try "variable" first (new standard for query/edit/more-info)
      Object variableObj = parameter.get(PARAMETERS_VARIABLE);
      if (variableObj != null) {
        name = String.valueOf(variableObj);
      }

      // Fall back to "name" (old standard, still used by basic tasks)
      if (!StringUtils.hasText(name)) {
        Object nameObj = parameter.get(PARAMETERS_NAME);
        if (nameObj != null) {
          name = String.valueOf(nameObj);
        }
      }

      // Additional fallback to label (used by TaskMoreInfoService)
      if (!StringUtils.hasText(name)) {
        Object labelObj = parameter.get(PARAMETERS_LABEL);
        if (labelObj != null) {
          name = String.valueOf(labelObj);
        }
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
   * How each client-visible task parameter is projected into the authorization profile payload.
   *
   * @see #toProfileParameterMap
   */
  public enum ProfileParameterShape {
    /**
     * External-link (URL) query tasks: {@link #toFeatureInfoParameter(TaskParameter)} (name, label,
     * value, optional type/required).
     */
    EXTERNAL_LINK_VIEWER,
    /**
     * SQL and web API query tasks: {@link #toQueryParameter(TaskParameter, String)} with default
     * type {@value org.sitmun.domain.DomainConstants.Tasks#TYPE_STRING}.
     */
    SIMPLE_STRING_DEFAULT,
    /**
     * Cartography query tasks: {@link #toServiceParameter(TaskParameter, String)} with default type
     * {@value org.sitmun.domain.DomainConstants.Tasks#TYPE_STRING}.
     */
    CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT,
    /**
     * Cartography edition tasks: {@link #toServiceParameter(TaskParameter, String)} with default
     * type {@value org.sitmun.domain.DomainConstants.Tasks#PARAM_TYPE_QUERY}.
     */
    EDIT_CARTOGRAPHY_WITH_VALUE_QUERY_DEFAULT
  }

  /**
   * Builds {@code name -> parameter DTO} for client configuration profiles. Skips backend-only
   * parameters ({@link TaskParameterType#isBackendOnly()}). Optionally omits URI template slots
   * (proxied {@code web-api-query}).
   *
   * <p>Returns an empty mutable map when no parameters are exposed. {@link
   * org.sitmun.authorization.client.dto.TaskDto} serializes omitted or empty maps as absent JSON
   * members ({@link com.fasterxml.jackson.annotation.JsonInclude.Include#NON_EMPTY}), matching the
   * previous nullable contract for wire format.
   *
   * <p>Values in the returned map are typed {@link
   * org.sitmun.authorization.client.dto.profile.ProfileParameter} records, but the public signature
   * remains {@code Map<String, Object>} for wire-format compatibility.
   *
   * @param parameters parsed task parameters
   * @param shape DTO projection per parameter
   * @param omitUriTemplatePlaceholders when {@code true}, drops parameters whose storage type is
   *     template ({@value org.sitmun.domain.DomainConstants.Tasks#PARAM_TYPE_TEMPLATE})
   * @return profile parameters map with typed record values, never {@code null} (possibly empty)
   */
  public Map<String, Object> toProfileParameterMap(
      List<TaskParameter> parameters,
      ProfileParameterShape shape,
      boolean omitUriTemplatePlaceholders) {
    Map<String, Object> result = new HashMap<>();

    for (TaskParameter param : parameters) {
      if (classify(param).isBackendOnly()) {
        continue;
      }
      if (omitUriTemplatePlaceholders && isUriTemplatePlaceholderType(param.type())) {
        continue;
      }
      Object dto =
          switch (shape) {
            case EXTERNAL_LINK_VIEWER -> toFeatureInfoParameter(param);
            case SIMPLE_STRING_DEFAULT -> toQueryParameter(param, TYPE_STRING);
            case CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT ->
                toServiceParameter(param, TYPE_STRING);
            case EDIT_CARTOGRAPHY_WITH_VALUE_QUERY_DEFAULT ->
                toServiceParameter(param, PARAM_TYPE_QUERY);
          };
      result.put(param.name(), dto);
    }

    return result;
  }

  private static boolean isUriTemplatePlaceholderType(@Nullable String type) {
    return type != null && PARAM_TYPE_TEMPLATE.equalsIgnoreCase(type.trim());
  }

  /**
   * Converts a single parameter to query parameter configuration: {@code {type, required}}.
   *
   * <p>Used with {@link #toProfileParameterMap} ({@link
   * ProfileParameterShape#SIMPLE_STRING_DEFAULT}).
   *
   * @param parameter the parameter to convert
   * @param defaultType default type if parameter.type() is null (e.g., "string", "query")
   * @return typed record with type and required fields
   */
  public QueryParameter toQueryParameter(TaskParameter parameter, String defaultType) {
    String type = parameter.type() != null ? parameter.type() : defaultType;
    boolean required = parameter.required() != null && parameter.required();
    return new QueryParameter(type, required);
  }

  /**
   * Converts a single parameter to DTO with optional value: {@code {type, required, value?}}.
   *
   * <p>Used with {@link #toProfileParameterMap} ({@link
   * ProfileParameterShape#CARTOGRAPHY_QUERY_WITH_VALUE_STRING_DEFAULT} and {@link
   * ProfileParameterShape#EDIT_CARTOGRAPHY_WITH_VALUE_QUERY_DEFAULT}).
   *
   * @param parameter the parameter to convert
   * @param defaultType default type if parameter.type() is null
   * @return typed record with type, required, and optional value fields
   */
  public ServiceParameter toServiceParameter(TaskParameter parameter, String defaultType) {
    String type = parameter.type() != null ? parameter.type() : defaultType;
    boolean required = parameter.required() != null && parameter.required();
    String value = parameter.rawValue();
    return new ServiceParameter(type, required, value);
  }

  /**
   * Converts a single parameter to more-info/feature-info field-forwarding DTO: {@code {label,
   * value, name, type?, required?}}.
   *
   * <p>Uses field property as value (falls back to rawValue). The {@code value} represents a
   * feature data field name for more-info tasks, enabling the viewer to extract and forward feature
   * attributes. Conditionally includes type and required fields.
   *
   * <p><strong>Contract Distinction:</strong> This shape is for more-info / feature-info
   * field-forwarding and external-link URL query profiles via {@link #toProfileParameterMap}
   * ({@link ProfileParameterShape#EXTERNAL_LINK_VIEWER}). Direct SQL / web-api query profiles use
   * {@link #toQueryParameter} instead (minimal {@code {type, required}}).
   *
   * <p>Do NOT switch direct query mappers to this method without first auditing that direct profile
   * consumers need the field-forwarding semantics.
   *
   * @param parameter the parameter to convert
   * @return typed record in Viewer-expected format for feature-field forwarding
   */
  public FeatureInfoParameter toFeatureInfoParameter(TaskParameter parameter) {
    String name = parameter.name();
    String label = parameter.name();
    String value = parameter.field() != null ? parameter.field() : parameter.rawValue();
    String type = parameter.type();
    Boolean required = parameter.required();
    return new FeatureInfoParameter(name, label, type, value, required);
  }
}
