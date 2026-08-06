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
import org.sitmun.administration.service.i18n.TemplateLiteralProcessor;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
  private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("<tr\\b[^>]*>[\\s\\S]*?</tr>");
  private static final Pattern TABLE_HEADER_CELL_PATTERN = Pattern.compile("(?i)<th\\b");
  private static final Pattern TABLE_DATA_CELL_PATTERN = Pattern.compile("(?i)<td\\b");
  private static final Pattern EACH_ROOT_PATTERN =
      Pattern.compile("\\{\\{#each\\s+([A-Za-z_][\\w]*)\\s*}}");
  private static final String TEMPLATE_ERROR_CLASS = "sitmun-template-error";
  private static final String TEMPLATE_KNOWN_CLASS = "sitmun-template-known";

  private final SystemVariableResolver systemVariableResolver;
  private final TemplateRequestCoordinatesService templateRequestCoordinatesService;
  private final TemplateContextNormalizer templateContextNormalizer;
  private final TemplateLiteralProcessor templateLiteralProcessor;
  private final CurrentRequestLanguageResolver currentRequestLanguageResolver;
  private final Handlebars handlebars = new Handlebars();

  public TemplatePreviewResponseDto renderPreview(
      String templateHtml, Map<String, Object> context) {
    return renderPreview(templateHtml, context, Collections.emptyList(), null, null, null);
  }

  public TemplatePreviewResponseDto renderPreview(
      String templateHtml, Map<String, Object> context, List<String> knownTaskReferences) {
    return renderPreview(templateHtml, context, knownTaskReferences, null, null, null);
  }

  public TemplatePreviewResponseDto renderPreview(
      String templateHtml,
      Map<String, Object> context,
      List<String> knownTaskReferences,
      String language) {
    return renderPreview(templateHtml, context, knownTaskReferences, language, null, null);
  }

  public TemplatePreviewResponseDto renderPreview(
      String templateHtml,
      Map<String, Object> context,
      List<String> knownTaskReferences,
      String language,
      Integer appId,
      Integer terId) {
    String source = templateHtml == null ? "" : templateHtml;
    Map<String, Object> safeContext = templateContextNormalizer.normalize(context);
    String withNormalizedEachBlocks = normalizeRootEachBlocks(source, safeContext);
    String withTableIterations = expandSitmunTableIterations(withNormalizedEachBlocks);
    String withExecutionHints =
        annotateUnresolvedTaskPlaceholders(withTableIterations, safeContext, knownTaskReferences);
    RequestCoordinates coordinates =
        appId != null || terId != null
            ? templateRequestCoordinatesService.buildOptional(appId, terId)
            : templateRequestCoordinatesService.buildForCurrentUser();
    String withBackendVars = replaceBackendVariables(withExecutionHints, coordinates);
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

    String bodyContent = bodyMatcher.group(2);
    String bodyReplacement =
        "<tbody"
            + bodyMatcher.group(1)
            + ">"
            + wrapDataRowsInEach(bodyContent, eachPath)
            + "</tbody>";
    return bodyMatcher.replaceFirst(Matcher.quoteReplacement(bodyReplacement));
  }

  /**
   * TipTap emits header cells as {@code <th>} rows inside {@code <tbody>} (no {@code <thead>}).
   * Keep those leading header rows outside {@code #each} so headers are not repeated per data row.
   */
  private String wrapDataRowsInEach(String bodyContent, String eachPath) {
    Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(bodyContent == null ? "" : bodyContent);
    StringBuilder headerRows = new StringBuilder();
    StringBuilder dataRows = new StringBuilder();
    boolean seenDataRow = false;
    int lastMatchEnd = 0;

    while (rowMatcher.find()) {
      if (rowMatcher.start() > lastMatchEnd) {
        String between = bodyContent.substring(lastMatchEnd, rowMatcher.start());
        if (seenDataRow) {
          dataRows.append(between);
        } else {
          headerRows.append(between);
        }
      }
      String row = rowMatcher.group();
      if (!seenDataRow && isHeaderRow(row)) {
        headerRows.append(row);
      } else {
        seenDataRow = true;
        dataRows.append(row);
      }
      lastMatchEnd = rowMatcher.end();
    }

    if (lastMatchEnd < bodyContent.length()) {
      String trailing = bodyContent.substring(lastMatchEnd);
      if (seenDataRow) {
        dataRows.append(trailing);
      } else {
        headerRows.append(trailing);
      }
    }

    if (dataRows.isEmpty()) {
      return "{{#each " + eachPath + "}}" + bodyContent + "{{/each}}";
    }

    return headerRows + "{{#each " + eachPath + "}}" + dataRows + "{{/each}}";
  }

  private boolean isHeaderRow(String rowHtml) {
    return TABLE_HEADER_CELL_PATTERN.matcher(rowHtml).find()
        && !TABLE_DATA_CELL_PATTERN.matcher(rowHtml).find();
  }

  private String replaceBackendVariables(String templateHtml, RequestCoordinates coordinates) {
    String html = templateHtml == null ? "" : templateHtml;
    Matcher matcher = BACKEND_VARIABLE_PATTERN.matcher(html);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String variableName = matcher.group(1);
      boolean attributeOrComment = isHtmlAttributeOrCommentContext(html, matcher.start());
      String replacement = systemVariableResolver.resolve("#{" + variableName + "}", coordinates);
      if (Objects.equals(replacement, "#{" + variableName + "}")) {
        if (isKnownSystemVariable(variableName)) {
          // Recognized var but no coords/value: bare name proves recognition.
          replacement = annotateForPreview(variableName, TEMPLATE_KNOWN_CLASS, attributeOrComment);
        } else {
          replacement =
              annotateForPreview(
                  "{{#" + variableName + "}}", TEMPLATE_ERROR_CLASS, attributeOrComment);
        }
      } else {
        replacement = opaqueInline(replacement);
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private boolean isKnownSystemVariable(String variableName) {
    Map<String, String> available = systemVariableResolver.getAvailableVariables();
    return available != null && available.containsKey(variableName);
  }

  private String annotateUnresolvedTaskPlaceholders(
      String templateHtml, Map<String, Object> context, List<String> knownTaskReferences) {
    Set<String> knownRoots = new LinkedHashSet<>(context.keySet());
    if (knownTaskReferences != null) {
      knownRoots.addAll(knownTaskReferences);
    }

    String html = templateHtml == null ? "" : templateHtml;
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(html);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String placeholderContent = matcher.group(1).trim();
      if (isKnownTaskPlaceholder(placeholderContent, knownRoots)
          && !isTaskPlaceholderResolved(placeholderContent, context)) {
        matcher.appendReplacement(
            sb,
            Matcher.quoteReplacement(
                markUnknownPlaceholder(
                    placeholderContent, isHtmlAttributeOrCommentContext(html, matcher.start()))));
        continue;
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * True when {@code index} lies inside an HTML attribute value or comment. Highlight spans must
   * not be spliced there — nested quotes terminate the attribute and spill markup into the
   * document.
   */
  static boolean isHtmlAttributeOrCommentContext(String html, int index) {
    if (html == null || index <= 0 || index > html.length()) {
      return false;
    }

    boolean inComment = false;
    boolean inTag = false;
    char attrQuote = 0;

    for (int i = 0; i < index; i++) {
      char c = html.charAt(i);
      if (inComment) {
        if (c == '-' && i + 2 < html.length() && html.startsWith("-->", i)) {
          inComment = false;
          i += 2;
        }
        continue;
      }
      if (!inTag) {
        if (c == '<' && i + 3 < html.length() && html.startsWith("<!--", i)) {
          inComment = true;
          i += 3;
        } else if (c == '<') {
          inTag = true;
        }
        continue;
      }
      if (attrQuote != 0) {
        if (c == attrQuote) {
          attrQuote = 0;
        }
        continue;
      }
      if (c == '"' || c == '\'') {
        attrQuote = c;
        continue;
      }
      if (c == '>') {
        inTag = false;
      }
    }

    return inComment || attrQuote != 0;
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

  private String markUnknownPlaceholder(String placeholderContent, boolean attributeOrComment) {
    String content = placeholderContent == null ? "" : placeholderContent;
    return annotateForPreview("{{" + content + "}}", TEMPLATE_ERROR_CLASS, attributeOrComment);
  }

  /**
   * Preview highlight span for text nodes; attribute/comment contexts get opaque text only (nested
   * quotes in spans break HTML attributes).
   */
  private static String annotateForPreview(
      String displayText, String cssClass, boolean attributeOrComment) {
    String opaque = opaqueInline(displayText);
    if (attributeOrComment) {
      return opaque;
    }
    return "<span class=\"" + cssClass + "\">" + opaque + "</span>";
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
