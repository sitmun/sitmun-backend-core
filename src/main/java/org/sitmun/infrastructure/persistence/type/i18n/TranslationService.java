package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TranslationService {

  @Value("${sitmun.language}")
  private String defaultLanguage;

  private final TranslationRepository translationRepository;
  private final EntityManager entityManager;

  TranslationService(TranslationRepository translationRepository, EntityManager entityManager) {
    this.translationRepository = translationRepository;
    this.entityManager = entityManager;
  }

  public void updateInternationalization(Object target) {
    String languageTag = LocaleContextHolder.getLocale().getLanguage();
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
      log.debug(
          "TranslationService.translate cache-hit entity={} entityId={}",
          entity,
          entityId);
      Map<String, String> byProperty = cache.lookup(entityId, entity);
      List<String> updates = new ArrayList<>();
      for (Map.Entry<String, String> e : byProperty.entrySet()) {
        if (!Strings.isEmpty(e.getValue())) {
          Object oldValue = wrapper.getPropertyValue(e.getKey());
          wrapper.setPropertyValue(e.getKey(), e.getValue());
          updates.add(String.format("%s: '%s' -> '%s'", e.getKey(), oldValue, e.getValue()));
        }
      }
      if (!updates.isEmpty()) {
        log.info(
            "Translations applied to {}:{} [{}]",
            entity,
            entityId,
            String.join(", ", updates));
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
      if (!Strings.isEmpty(translation.getTranslation())) {
        Object oldValue = wrapper.getPropertyValue(property);
        wrapper.setPropertyValue(property, translation.getTranslation());
        updates.add(String.format("%s: '%s' -> '%s'", property, oldValue, translation.getTranslation()));
      }
    }
    if (!updates.isEmpty()) {
      log.info(
          "Translations applied to {}:{} [{}]",
          entity,
          entityId,
          String.join(", ", updates));
    }
  }
}
