package org.sitmun.administration.service.i18n;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TemplateLiteralProcessor {

  private static final Pattern TAG_PATTERN = Pattern.compile("<t>(.*?)</t>", Pattern.DOTALL);

  private final LiteralTranslationResolver literalTranslationResolver;

  public String process(String html, String language) {
    if (!StringUtils.hasText(html) || !html.contains("<t>")) {
      return html;
    }

    Matcher matcher = TAG_PATTERN.matcher(html);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String literal = matcher.group(1);
      String replacement = literalTranslationResolver.resolve(literal, language);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
