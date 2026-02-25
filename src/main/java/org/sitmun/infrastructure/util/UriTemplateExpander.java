package org.sitmun.infrastructure.util;

import com.damnhandy.uri.template.MalformedUriTemplateException;
import com.damnhandy.uri.template.UriTemplate;
import java.util.*;
import lombok.Value;

/**
 * Utility class for expanding URI templates using RFC 6570 standard. ONLY handles {variable} syntax
 * for URIs.
 *
 * <p>This class provides methods to expand URI templates according to RFC 6570 specification using
 * the Handy URI Templates library.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6570">RFC 6570: URI Template</a>
 */
public final class UriTemplateExpander {

  private UriTemplateExpander() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Expands a URI template with the given parameters using RFC 6570 standard.
   *
   * @param template The URI template string with {variable} syntax
   * @param parameters Map of parameter names to values
   * @return Expanded URI string
   * @throws MalformedUriTemplateException if template is invalid
   * @throws IllegalArgumentException if template or parameters are null
   */
  public static String expand(String template, Map<String, String> parameters) {
    if (template == null) {
      throw new IllegalArgumentException("Template cannot be null");
    }
    if (parameters == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }

    if (parameters.isEmpty() || !hasTemplateVariables(template)) {
      return template;
    }

    UriTemplate uriTemplate = UriTemplate.fromTemplate(template);
    parameters.forEach(uriTemplate::set);
    return uriTemplate.expand();
  }

  /**
   * Expands template and returns both the expanded string and set of variables that were
   * successfully expanded.
   *
   * @param template The URI template string
   * @param parameters Map of parameter names to values
   * @return ExpandedResult containing URI and used variable names
   * @throws MalformedUriTemplateException if template is invalid
   * @throws IllegalArgumentException if template or parameters are null
   */
  public static ExpandedResult expandWithUsedVariables(
      String template, Map<String, String> parameters) {
    if (template == null) {
      throw new IllegalArgumentException("Template cannot be null");
    }
    if (parameters == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }

    if (parameters.isEmpty() || !hasTemplateVariables(template)) {
      return new ExpandedResult(template, Collections.emptySet());
    }

    UriTemplate uriTemplate = UriTemplate.fromTemplate(template);
    Set<String> templateVars = new HashSet<>(Arrays.asList(uriTemplate.getVariables()));
    Set<String> usedVars = new HashSet<>();

    parameters.forEach(
        (key, value) -> {
          if (templateVars.contains(key)) {
            uriTemplate.set(key, value);
            usedVars.add(key);
          }
        });

    String expandedUri = uriTemplate.expand();
    return new ExpandedResult(expandedUri, usedVars);
  }

  /**
   * Checks if a string contains URI template variables {variable}.
   *
   * @param template String to check
   * @return true if contains {variable} patterns
   */
  public static boolean hasTemplateVariables(String template) {
    if (template == null || template.isEmpty()) {
      return false;
    }
    // Simple check for {variable} pattern
    return template.contains("{") && template.contains("}");
  }

  /**
   * Gets the list of variable names present in a URI template.
   *
   * @param template The URI template string
   * @return Set of variable names found in the template
   * @throws MalformedUriTemplateException if template is invalid
   * @throws IllegalArgumentException if template is null
   */
  public static Set<String> getVariableNames(String template) {
    if (template == null) {
      throw new IllegalArgumentException("Template cannot be null");
    }

    if (!hasTemplateVariables(template)) {
      return Collections.emptySet();
    }

    UriTemplate uriTemplate = UriTemplate.fromTemplate(template);
    return new HashSet<>(Arrays.asList(uriTemplate.getVariables()));
  }

  /** Result of template expansion with tracking of which variables were used. */
  @Value
  public static class ExpandedResult {
    String uri;
    Set<String> usedVariables;
  }
}
