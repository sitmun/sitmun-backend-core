package org.sitmun.administration.service.i18n;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.persistence.type.i18n.DatabaseDefaultLanguageResolver;
import org.sitmun.infrastructure.persistence.type.i18n.Language;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValue;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiteralTranslationEnsureService {

  private static final Pattern TAG_PATTERN = Pattern.compile("<t>(.*?)</t>", Pattern.DOTALL);

  private final LiteralTranslationRepository literalTranslationRepository;
  private final LiteralTranslationValueRepository literalTranslationValueRepository;
  private final DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver;

  public static Set<String> extractLiteralKeys(String html) {
    Set<String> keys = new LinkedHashSet<>();
    if (!StringUtils.hasText(html) || !html.contains("<t>")) {
      return keys;
    }
    Matcher matcher = TAG_PATTERN.matcher(html);
    while (matcher.find()) {
      String key = matcher.group(1);
      if (StringUtils.hasText(key)) {
        keys.add(key);
      }
    }
    return keys;
  }

  @Transactional
  public void ensureLiteralsFromTemplateHtml(String templateHtml) {
    Set<String> keys = extractLiteralKeys(templateHtml);
    if (keys.isEmpty()) {
      return;
    }
    Language sourceLanguage = databaseDefaultLanguageResolver.requireLanguage();
    for (String key : keys) {
      ensureLiteral(key, sourceLanguage);
    }
  }

  @Transactional
  public void ensureLiteral(String literal, Language sourceLanguage) {
    if (!StringUtils.hasText(literal) || sourceLanguage == null) {
      return;
    }
    Optional<LiteralTranslation> existing = literalTranslationRepository.findByLiteral(literal);
    if (existing.isPresent()) {
      syncSourceValueIfMissing(existing.get(), literal);
      return;
    }
    try {
      LiteralTranslation saved =
          literalTranslationRepository.save(
              LiteralTranslation.builder().literal(literal).sourceLanguage(sourceLanguage).build());
      upsertValue(saved, sourceLanguage, literal);
    } catch (DataIntegrityViolationException ex) {
      log.debug("Literal already exists concurrently: {}", literal);
      literalTranslationRepository
          .findByLiteral(literal)
          .ifPresent(found -> syncSourceValueIfMissing(found, literal));
    }
  }

  private void syncSourceValueIfMissing(LiteralTranslation literalTranslation, String literal) {
    Language source = literalTranslation.getSourceLanguage();
    if (source == null || !StringUtils.hasText(source.getShortname())) {
      return;
    }
    Optional<LiteralTranslationValue> existing =
        literalTranslationValueRepository.findByLiteralTranslationIdAndLanguageShortname(
            literalTranslation.getId(), source.getShortname());
    if (existing.isEmpty()) {
      upsertValue(literalTranslation, source, literal);
    }
  }

  private void upsertValue(LiteralTranslation literalTranslation, Language language, String value) {
    LiteralTranslationValue row =
        literalTranslationValueRepository
            .findByLiteralTranslationIdAndLanguageShortname(
                literalTranslation.getId(), language.getShortname())
            .orElseGet(LiteralTranslationValue::new);
    row.setLiteralTranslation(literalTranslation);
    row.setLanguage(language);
    row.setValue(value);
    literalTranslationValueRepository.save(row);
  }
}
