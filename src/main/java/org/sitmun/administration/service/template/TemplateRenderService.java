package org.sitmun.administration.service.template;

import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TemplateRenderService {

  private static final Pattern BACKEND_VARIABLE_PATTERN = Pattern.compile("\\{\\{#([A-Z_]+)}}");
  private static final Pattern PARAMETER_LOOKUP_PATTERN = Pattern.compile("\\{\\{(task_\\d+)\\.(\\$[A-Za-z0-9_]+)}}");
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^{}]+)}}");
  private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("([A-Za-z0-9_$.]+)\\[(\\d+)]");

  private final SystemVariableResolver systemVariableResolver;
  private final Handlebars handlebars = new Handlebars();

  public TemplatePreviewResponseDto renderPreview(String templateHtml, Map<String, Object> context) {
    String source = templateHtml == null ? "" : templateHtml;
    String withBackendVars = replaceBackendVariables(source);
    String withArrayIndexes = normalizeArrayIndexes(withBackendVars);
    String normalized = normalizeParameterLookups(withArrayIndexes);
    List<String> placeholders = extractPlaceholders(source);

    try {
      Template compiled = handlebars.compileInline(normalized);
      String html = compiled.apply(context == null ? Collections.emptyMap() : context);
      return TemplatePreviewResponseDto.builder().html(html).placeholders(placeholders).build();
    } catch (HandlebarsException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Template preview contains invalid Handlebars syntax: " + e.getMessage(),
          e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to render template preview", e);
    }
  }

  private String replaceBackendVariables(String templateHtml) {
    Matcher matcher = BACKEND_VARIABLE_PATTERN.matcher(templateHtml);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String variableName = matcher.group(1);
      String replacement = systemVariableResolver.resolve("#{" + variableName + "}", new RequestCoordinates());
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private String normalizeParameterLookups(String templateHtml) {
    Matcher matcher = PARAMETER_LOOKUP_PATTERN.matcher(templateHtml);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(buffer, Matcher.quoteReplacement("{{lookup " + matcher.group(1) + " \"" + matcher.group(2) + "\"}}"));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private String normalizeArrayIndexes(String templateHtml) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateHtml == null ? "" : templateHtml);
    StringBuffer buffer = new StringBuffer();

    while (matcher.find()) {
      String placeholderContent = matcher.group(1);
      String normalizedPlaceholder = normalizeArrayIndexesInPlaceholder(placeholderContent);
      matcher.appendReplacement(buffer, Matcher.quoteReplacement("{{" + normalizedPlaceholder + "}}"));
    }

    matcher.appendTail(buffer);
    return buffer.toString();
  }

  private String normalizeArrayIndexesInPlaceholder(String placeholderContent) {
    Matcher matcher = ARRAY_INDEX_PATTERN.matcher(placeholderContent);
    StringBuffer buffer = new StringBuffer();

    while (matcher.find()) {
      matcher.appendReplacement(
          buffer, Matcher.quoteReplacement(matcher.group(1) + ".[" + matcher.group(2) + "]"));
    }

    matcher.appendTail(buffer);
    return buffer.toString();
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
