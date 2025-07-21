package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.persistence.Id;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

@Component
@Slf4j
public class TranslationService {

  @Value("${sitmun.language}")
  private String defaultLanguage;

  private final TranslationRepository translationRepository;

  TranslationService(TranslationRepository translationRepository) {
    this.translationRepository = translationRepository;
  }

  public void updateInternationalization(Object target) {
    String languageTag = LocaleContextHolder.getLocale().getLanguage();
    if (!Objects.equals(defaultLanguage, languageTag)) {
      ConfigurablePropertyAccessor wrapper = PropertyAccessorFactory.forDirectFieldAccess(target);

      Optional<String> idName = Arrays.stream(target.getClass().getDeclaredFields())
        .filter(it -> it.isAnnotationPresent(Id.class))
        .findFirst()
        .map(Field::getName);

      idName.ifPresent(s -> translate(target, wrapper, s, languageTag));
    }
  }

  private void translate(Object target, ConfigurablePropertyAccessor wrapper, String idName, String languageTag) {
    Integer entityId = (Integer) wrapper.getPropertyValue(idName);
    String entity = target.getClass().getSimpleName();
    List<Translation> translations = translationRepository.findTranslation(entityId, entity, languageTag);
    List<String> translatedProperties = new ArrayList<>();
    for (Translation translation : translations) {
      if (translation.getColumn() == null) {
        log.warn("Translations for {}:{}: translation {} has no column defined", entity, entityId, translation.getId());
        continue;
      }
      String property = translation.getColumn().replace(entity + ".", "");
      if (!Strings.isEmpty(translation.getTranslation())) {
        translatedProperties.add(property);
        wrapper.setPropertyValue(property, translation.getTranslation());
      }
    }
    if (!translatedProperties.isEmpty()) {
      log.info("Translations for {}:{} for properties {}", entity, entityId, String.join(", ", translatedProperties.toArray(new String[0])));
    }
  }
}
