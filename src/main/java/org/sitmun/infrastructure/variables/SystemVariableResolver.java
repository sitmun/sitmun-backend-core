package org.sitmun.infrastructure.variables;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
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
   * Resolves all system variables in the given template string.
   *
   * @param template Template string containing #{VARIABLE_NAME} placeholders
   * @param user User context for resolution
   * @param territory Territory context for resolution
   * @param application Application context for resolution
   * @return Template with all system variables resolved to their values
   */
  public String resolve(String template, User user, Territory territory, Application application) {
    if (template == null || template.isEmpty()) {
      return template;
    }

    // Build the evaluation context with available entities
    EvaluationContext context = createEvaluationContext(user, territory, application);

    // Find all system variable references
    Matcher matcher = SYSTEM_VAR_PATTERN.matcher(template);
    StringBuffer result = new StringBuffer();

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
   * @param user User entity
   * @param territory Territory entity
   * @param application Application entity
   * @return Evaluation context with entities registered as variables
   */
  private EvaluationContext createEvaluationContext(
      User user, Territory territory, Application application) {
    StandardEvaluationContext context = new StandardEvaluationContext();

    if (user != null) {
      context.setVariable("user", user);
    }
    if (territory != null) {
      context.setVariable("territory", territory);
    }
    if (application != null) {
      context.setVariable("application", application);
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
