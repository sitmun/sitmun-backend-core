package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TranslationService {

  private final TranslationRepository translationRepository;
  private final EntityManager entityManager;
  private final DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver;

  TranslationService(
      TranslationRepository translationRepository,
      EntityManager entityManager,
      DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver) {
    this.translationRepository = translationRepository;
    this.entityManager = entityManager;
    this.databaseDefaultLanguageResolver = databaseDefaultLanguageResolver;
  }

  public void updateInternationalization(Object target) {
    // Use full BCP-47 tag (e.g. "oc-aranes"), not getLanguage() which strips variants to "oc".
    String languageTag = LocaleContextHolder.getLocale().toLanguageTag();
    String defaultLanguage = databaseDefaultLanguageResolver.resolveShortname();
    log.debug(
        "TranslationService.updateInternationalization targetClass={} languageTag={} defaultLanguage={}",
        target != null ? target.getClass().getName() : "null",
        languageTag,
        defaultLanguage);
    if (!Objects.equals(defaultLanguage, languageTag)) {
      ConfigurablePropertyAccessor wrapper = PropertyAccessorFactory.forDirectFieldAccess(target);

      Optional<String> idName =
          Arrays.stream(target.getClass().getDeclaredFields())
              .filter(it -> it.isAnnotationPresent(Id.class))
              .findFirst()
              .map(Field::getName);

      if (idName.isPresent()) {
        translate(target, wrapper, idName.get(), languageTag);
      } else {
        log.debug(
            "TranslationService.skip targetClass={} reason=no-@Id-field",
            target != null ? target.getClass().getName() : "null");
      }

      // Mark entity as read-only to prevent Hibernate from generating UPDATE statements
      // while still allowing lazy loading and serialization
      if (entityManager.contains(target)) {
        entityManager.unwrap(org.hibernate.Session.class).setReadOnly(target, true);
      }
    }
  }

  private void translate(
      Object target, ConfigurablePropertyAccessor wrapper, String idName, String languageTag) {
    Integer entityId = (Integer) wrapper.getPropertyValue(idName);
    String entity = target.getClass().getSimpleName();
    log.debug(
        "TranslationService.translate targetClass={} entity={} entityId={} idField={} languageTag={}",
        target.getClass().getName(),
        entity,
        entityId,
        idName,
        languageTag);

    TranslationCache cache = TranslationCache.fromRequest();
    if (cache != null && cache.isPopulated()) {
      log.debug("TranslationService.translate cache-hit entity={} entityId={}", entity, entityId);
      Map<String, String> byProperty = cache.lookup(entityId, entity);
      List<String> updates = new ArrayList<>();
      for (Map.Entry<String, String> e : byProperty.entrySet()) {
        applyTranslationValue(target, wrapper, e.getKey(), e.getValue(), updates);
      }
      if (!updates.isEmpty()) {
        log.debug(
            "Translations applied to {}:{} [{}]", entity, entityId, String.join(", ", updates));
      }
      return;
    }

    log.debug(
        "TranslationService.translate cache-miss entity={} entityId={} -> fallback repository query",
        entity,
        entityId);

    List<Translation> translations =
        translationRepository.findTranslation(entityId, entity, languageTag);
    List<String> updates = new ArrayList<>();
    for (Translation translation : translations) {
      if (translation.getColumn() == null) {
        log.warn(
            "Translations for {}:{}: translation {} has no column defined",
            entity,
            entityId,
            translation.getId());
        continue;
      }
      String property = translation.getColumn().replace(entity + ".", "");
      applyTranslationValue(target, wrapper, property, translation.getTranslation(), updates);
    }
    if (!updates.isEmpty()) {
      log.debug("Translations applied to {}:{} [{}]", entity, entityId, String.join(", ", updates));
    }
  }

  /**
   * Language.endonym ({@code name}) must never be overwritten by {@code ?lang=}. Locale labels go
   * to read-only {@link Language#getTranslatedName()}.
   */
  private static void applyTranslationValue(
      Object target,
      ConfigurablePropertyAccessor wrapper,
      String property,
      String value,
      List<String> updates) {
    if (Strings.isEmpty(value) || property == null) {
      return;
    }
    String targetProperty = resolveTargetProperty(target, property);
    if (targetProperty == null) {
      return;
    }
    Object oldValue = wrapper.getPropertyValue(targetProperty);
    wrapper.setPropertyValue(targetProperty, value);
    updates.add(String.format("%s: '%s' -> '%s'", targetProperty, oldValue, value));
  }

  private static String resolveTargetProperty(Object target, String property) {
    if (target instanceof Language) {
      return "name".equals(property) ? "translatedName" : null;
    }
    return property;
  }
}
