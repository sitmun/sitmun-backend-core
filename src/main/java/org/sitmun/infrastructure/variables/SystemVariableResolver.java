package org.sitmun.infrastructure.variables;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.config.SystemVariableProperties;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

/**
 * Service for resolving system variables using Spring Expression Language (SpEL). System variables
 * use #{} syntax and are resolved from User, Territory, and Application entities.
 *
 * <p>Example configuration in application.yml:
 *
 * <pre>
 * sitmun:
 *   variables:
 *     system:
 *       USER_ID: "#{user.id}"
 *       TERR_ID: "#{territory.id}"
 *       TERR_COD: "#{territory.code}"
 *       APP_ID: "#{application.id}"
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemVariableResolver {

  private final SystemVariableProperties properties;
  private final ExpressionParser parser = new SpelExpressionParser();

  /** Pattern to match system variable placeholders: #{VARIABLE_NAME} */
  private static final Pattern SYSTEM_VAR_PATTERN = Pattern.compile("#\\{([A-Z_]+)\\}");

  /**
   * Resolves all system variables using {@link RequestCoordinates} (user, territory, application).
   *
   * @param template template string containing {@code #{VARIABLE_NAME}} placeholders
   * @param coordinates request context; if {@code null}, resolves with no entity bindings
   * @return template with placeholders replaced where possible
   */
  public String resolve(String template, RequestCoordinates coordinates) {
    if (template == null || template.isEmpty()) {
      return template;
    }

    // Build the evaluation context with available entities
    EvaluationContext context = createEvaluationContext(coordinates);

    // Find all system variable references
    Matcher matcher = SYSTEM_VAR_PATTERN.matcher(template);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String variableName = matcher.group(1);
      String replacement = resolveVariable(variableName, context);
      matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(result);

    return result.toString();
  }

  /**
   * Resolves a single system variable to its value.
   *
   * @param variableName Name of the system variable (e.g., "USER_ID")
   * @param context SpEL evaluation context
   * @return Resolved value as string, or the original placeholder if resolution fails
   */
  private String resolveVariable(String variableName, EvaluationContext context) {
    String expression = properties.getSystem().get(variableName);

    if (expression == null) {
      log.warn("System variable '{}' not found in configuration", variableName);
      return "#{" + variableName + "}"; // Return unchanged if not configured
    }

    try {
      // Remove #{} wrapper if present in the expression definition
      String cleanExpression = expression.replaceAll("^#\\{(.+)\\}$", "$1");
      Expression exp = parser.parseExpression(cleanExpression);
      Object value = exp.getValue(context);

      return value != null ? value.toString() : "";
    } catch (Exception e) {
      log.error(
          "Failed to resolve system variable '{}' with expression '{}': {}",
          variableName,
          expression,
          e.getMessage());
      return "#{" + variableName + "}"; // Return unchanged on error
    }
  }

  /**
   * Creates a SpEL evaluation context with the provided entities.
   *
   * @param coordinates user, territory, and application to expose as SpEL variables; {@code null}
   *     yields an empty context
   * @return evaluation context with non-null entities registered as {@code user}, {@code
   *     territory}, and {@code application}
   */
  private EvaluationContext createEvaluationContext(RequestCoordinates coordinates) {
    StandardEvaluationContext context = new StandardEvaluationContext();
    if (coordinates == null) {
      return context;
    }

    if (coordinates.getUser() != null) {
      context.setVariable("user", coordinates.getUser());
    }
    if (coordinates.getTerritory() != null) {
      context.setVariable("territory", coordinates.getTerritory());
    }
    if (coordinates.getApplication() != null) {
      context.setVariable("application", coordinates.getApplication());
    }
    return context;
  }

  /**
   * Returns a map of all configured system variable names and their value expressions. Useful for
   * admin UI autocomplete and documentation.
   *
   * @return Map of variable name -> SpEL expression
   */
  public Map<String, String> getAvailableVariables() {
    return new HashMap<>(properties.getSystem());
  }

  /**
   * Checks if a string contains any system variable references.
   *
   * @param template String to check
   * @return true if contains #{...} pattern
   */
  public static boolean containsSystemVariables(String template) {
    return template != null && SYSTEM_VAR_PATTERN.matcher(template).find();
  }
}
