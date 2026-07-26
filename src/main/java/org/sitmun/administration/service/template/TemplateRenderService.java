package org.sitmun.administration.service.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.service.i18n.CurrentRequestLanguageResolver;
import org.sitmun.administration.service.i18n.LiteralTranslationResolver;
import org.sitmun.administration.service.i18n.TemplateLiteralProcessor;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
public class TemplateRenderService {

  private static final String HANDLEBARS_OPEN = "&#123;&#123;";
  private static final String HANDLEBARS_CLOSE = "&#125;&#125;";

  private static final Pattern BACKEND_VARIABLE_PATTERN = Pattern.compile("\\{\\{#([A-Z_]+)}}");
  private static final Pattern PARAMETER_LOOKUP_PATTERN =
      Pattern.compile("\\{\\{([A-Za-z_][\\w]*)\\.(\\$[\\w]+)}}");
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^{}]+)}}");
  private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("([\\w$.]+)\\[(\\d+)]");
  private static final Pattern PATH_SEGMENT_PATTERN = Pattern.compile("([^.\\[\\]]+)|\\[(\\d+)]");
  private static final Pattern HTML_RESULT_PLACEHOLDER_PATTERN =
      Pattern.compile("\\{\\{([A-Za-z_][\\w]*\\.html)}}");
  private static final Pattern SITMUN_TABLE_ITERATION_PATTERN =
      Pattern.compile("<table\\b[^>]*>[\\s\\S]*?</table>");
  private static final Pattern SITMUN_TABLE_EACH_ATTRIBUTE_PATTERN =
      Pattern.compile("\\sdata-sitmun-each=\"([A-Za-z_]\\w*(?:\\.[A-Za-z_][\\w]*)*)\"");
  private static final Pattern SITMUN_TEMPORARY_TABLE_PATTERN =
      Pattern.compile("<temporary\\b[^>]*>[\\s\\S]*?</temporary>");
  private static final Pattern TABLE_BODY_PATTERN =
      Pattern.compile("<tbody([^>]*)>([\\s\\S]*?)</tbody>");
  private static final Pattern EACH_ROOT_PATTERN =
      Pattern.compile("\\{\\{#each\\s+([A-Za-z_][\\w]*)\\s*}}");
  private static final String TASK_NOT_EXECUTED_LITERAL = "task not executed";

  private final SystemVariableResolver systemVariableResolver;
  private final TemplateRequestCoordinatesService templateRequestCoordinatesService;
  private final TemplateContextNormalizer templateContextNormalizer;
  private final TemplateLiteralProcessor templateLiteralProcessor;
  private final CurrentRequestLanguageResolver currentRequestLanguageResolver;
  private final LiteralTranslationResolver literalTranslationResolver;
  private final Handlebars handlebars = new Handlebars();

  public TemplatePreviewResponseDto renderPreview(
      String templateHtml, Map<String, Object> context) {
    return renderPreview(templateHtml, context, Collections.emptyList(), null);
  }

  public TemplatePreviewResponseDto renderPreview(
      String templateHtml, Map<String, Object> context, List<String> knownTaskReferences) {
    return renderPreview(templateHtml, context, knownTaskReferences, null);
  }

  public TemplatePreviewResponseDto renderPreview(
      String templateHtml,
      Map<String, Object> context,
      List<String> knownTaskReferences,
      String language) {
    String source = templateHtml == null ? "" : templateHtml;
    Map<String, Object> safeContext = templateContextNormalizer.normalize(context);
    String withNormalizedEachBlocks = normalizeRootEachBlocks(source, safeContext);
    String withTableIterations = expandSitmunTableIterations(withNormalizedEachBlocks);
    String withExecutionHints =
        annotateUnresolvedTaskPlaceholders(withTableIterations, safeContext, knownTaskReferences);
    String withBackendVars =
        replaceBackendVariables(
            withExecutionHints, templateRequestCoordinatesService.buildForCurrentUser());
    String withArrayIndexes = normalizeArrayIndexes(withBackendVars);
    String withHtmlResults = normalizeHtmlResultPlaceholders(withArrayIndexes);
    String normalized = normalizeParameterLookups(withHtmlResults);
    List<String> placeholders = extractPlaceholders(source);

    try {
      Template compiled = handlebars.compileInline(normalized);
      String html = compiled.apply(safeContext);
      String renderedLanguage =
          language != null ? language : currentRequestLanguageResolver.resolve(this);
      String translatedHtml = templateLiteralProcessor.process(html, renderedLanguage);
      return TemplatePreviewResponseDto.builder()
          .html(translatedHtml)
          .placeholders(placeholders)
          .build();
    } catch (HandlebarsException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Template preview contains invalid Handlebars syntax: " + e.getMessage(),
          e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to render template preview", e);
    }
  }

  private String normalizeRootEachBlocks(String templateHtml, Map<String, Object> context) {
    Matcher matcher = EACH_ROOT_PATTERN.matcher(templateHtml == null ? "" : templateHtml);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String rootKey = matcher.group(1);
      if (hasRowsArray(context.get(rootKey))) {
        matcher.appendReplacement(sb, Matcher.quoteReplacement("{{#each " + rootKey + ".rows}}"));
        continue;
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private boolean hasRowsArray(Object value) {
    if (!(value instanceof Map<?, ?> mapValue)) {
      return false;
    }
    return mapValue.get("rows") instanceof List<?>;
  }

  private String expandSitmunTableIterations(String templateHtml) {
    Matcher matcher =
        SITMUN_TABLE_ITERATION_PATTERN.matcher(templateHtml == null ? "" : templateHtml);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(
          sb, Matcher.quoteReplacement(expandSitmunTableIteration(matcher.group())));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String expandSitmunTableIteration(String tableHtml) {
    Matcher eachMatcher = SITMUN_TABLE_EACH_ATTRIBUTE_PATTERN.matcher(tableHtml);
    if (!eachMatcher.find()) {
      return tableHtml;
    }

    String eachPath = eachMatcher.group(1);
    String normalizedTable = SITMUN_TEMPORARY_TABLE_PATTERN.matcher(tableHtml).replaceAll("");
    normalizedTable = SITMUN_TABLE_EACH_ATTRIBUTE_PATTERN.matcher(normalizedTable).replaceAll("");

    Matcher bodyMatcher = TABLE_BODY_PATTERN.matcher(normalizedTable);
    if (!bodyMatcher.find()) {
      return normalizedTable;
    }

    String bodyReplacement =
        "<tbody"
            + bodyMatcher.group(1)
            + ">{{#each "
            + eachPath
            + "}}"
            + bodyMatcher.group(2)
            + "{{/each}}</tbody>";
    return bodyMatcher.replaceFirst(Matcher.quoteReplacement(bodyReplacement));
  }

  private String replaceBackendVariables(String templateHtml, RequestCoordinates coordinates) {
    Matcher matcher = BACKEND_VARIABLE_PATTERN.matcher(templateHtml);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String variableName = matcher.group(1);
      String replacement = systemVariableResolver.resolve("#{" + variableName + "}", coordinates);
      if (Objects.equals(replacement, "#{" + variableName + "}")) {
        replacement = escapeHandlebarsPlaceholder("#" + variableName);
      } else {
        replacement = opaqueInline(replacement);
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String annotateUnresolvedTaskPlaceholders(
      String templateHtml, Map<String, Object> context, List<String> knownTaskReferences) {
    Set<String> knownRoots = new LinkedHashSet<>(context.keySet());
    if (knownTaskReferences != null) {
      knownRoots.addAll(knownTaskReferences);
    }

    String language = currentRequestLanguageResolver.resolve(this);
    String taskNotExecutedHint = opaqueInline(resolveLiteral(TASK_NOT_EXECUTED_LITERAL, language));
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateHtml == null ? "" : templateHtml);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String placeholderContent = matcher.group(1).trim();
      if (isKnownTaskPlaceholder(placeholderContent, knownRoots)
          && !isTaskPlaceholderResolved(placeholderContent, context)) {
        matcher.appendReplacement(
            sb,
            Matcher.quoteReplacement(
                escapeHandlebarsPlaceholder(placeholderContent)
                    + " ("
                    + taskNotExecutedHint
                    + ")"));
        continue;
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String resolveLiteral(String key, String language) {
    String resolved = literalTranslationResolver.resolve(key, language);
    return StringUtils.hasText(resolved) ? resolved : key;
  }

  private boolean isKnownTaskPlaceholder(String placeholderContent, Set<String> knownRoots) {
    if (placeholderContent.isBlank()
        || placeholderContent.startsWith("#")
        || placeholderContent.startsWith("/")
        || placeholderContent.contains(" ")) {
      return false;
    }

    String rootKey = extractRootKey(placeholderContent);
    return knownRoots.contains(rootKey) || rootKey.startsWith("task_");
  }

  private boolean isTaskPlaceholderResolved(
      String placeholderContent, Map<String, Object> context) {
    int firstDot = placeholderContent.indexOf('.');
    String rootKey = extractRootKey(placeholderContent);
    if (!context.containsKey(rootKey)) {
      return false;
    }
    if (firstDot < 0) {
      return true;
    }

    Object current = context.get(rootKey);
    String remainingPath = placeholderContent.substring(firstDot + 1);
    Matcher matcher = PATH_SEGMENT_PATTERN.matcher(remainingPath);
    while (matcher.find()) {
      String propertyName = matcher.group(1);
      String indexValue = matcher.group(2);

      if (propertyName != null) {
        if (!(current instanceof Map<?, ?> mapValue) || !mapValue.containsKey(propertyName)) {
          return false;
        }
        current = mapValue.get(propertyName);
        continue;
      }

      int index = Integer.parseInt(indexValue);
      if (!(current instanceof List<?> listValue) || index < 0 || index >= listValue.size()) {
        return false;
      }
      current = listValue.get(index);
    }

    return true;
  }

  private String extractRootKey(String placeholderContent) {
    int firstDot = placeholderContent.indexOf('.');
    int firstBracket = placeholderContent.indexOf('[');
    int endIndex = placeholderContent.length();
    if (firstDot >= 0) {
      endIndex = Math.min(endIndex, firstDot);
    }
    if (firstBracket >= 0) {
      endIndex = Math.min(endIndex, firstBracket);
    }
    return placeholderContent.substring(0, endIndex);
  }

  private String escapeHandlebarsPlaceholder(String placeholderContent) {
    String content = placeholderContent == null ? "" : placeholderContent;
    return "<span class=\"sitmun-template-placeholder\">"
        + opaqueInline("{{" + content + "}}")
        + "</span>";
  }

  /** HTML-escape and neutralize Handlebars delimiters for safe splice into compileInline source. */
  private static String opaqueInline(String value) {
    String escaped = HtmlUtils.htmlEscape(value == null ? "" : value);
    return escaped.replace("{{", HANDLEBARS_OPEN).replace("}}", HANDLEBARS_CLOSE);
  }

  private String normalizeParameterLookups(String templateHtml) {
    Matcher matcher = PARAMETER_LOOKUP_PATTERN.matcher(templateHtml);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(
          sb,
          Matcher.quoteReplacement(
              "{{lookup " + matcher.group(1) + " \"" + matcher.group(2) + "\"}}"));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String normalizeArrayIndexes(String templateHtml) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateHtml == null ? "" : templateHtml);
    StringBuilder sb = new StringBuilder();

    while (matcher.find()) {
      String placeholderContent = matcher.group(1);
      String normalizedPlaceholder = normalizeArrayIndexesInPlaceholder(placeholderContent);
      matcher.appendReplacement(sb, Matcher.quoteReplacement("{{" + normalizedPlaceholder + "}}"));
    }

    matcher.appendTail(sb);
    return sb.toString();
  }

  private String normalizeHtmlResultPlaceholders(String templateHtml) {
    Matcher matcher =
        HTML_RESULT_PLACEHOLDER_PATTERN.matcher(templateHtml == null ? "" : templateHtml);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(sb, Matcher.quoteReplacement("{{{" + matcher.group(1) + "}}}"));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String normalizeArrayIndexesInPlaceholder(String placeholderContent) {
    Matcher matcher = ARRAY_INDEX_PATTERN.matcher(placeholderContent);
    StringBuilder sb = new StringBuilder();

    while (matcher.find()) {
      matcher.appendReplacement(
          sb, Matcher.quoteReplacement(matcher.group(1) + ".[" + matcher.group(2) + "]"));
    }

    matcher.appendTail(sb);
    return sb.toString();
  }

  private List<String> extractPlaceholders(String templateHtml) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateHtml == null ? "" : templateHtml);
    List<String> placeholders = new ArrayList<>();
    while (matcher.find()) {
      placeholders.add(matcher.group(1).trim());
    }
    return placeholders;
  }
}
