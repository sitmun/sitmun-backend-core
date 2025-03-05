package org.sitmun.infrastructure.persistence.type.i18n;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Slf4j
public class TranslationService {

  @Value("${sitmun.language}")
  private String defaultLanguage;

  private TranslationRepository translationRepository;

  TranslationService(TranslationRepository translationRepository) {
    this.translationRepository = translationRepository;
  }

  public void updateInternationalization(Object target) {
    String languageTag = LocaleContextHolder.getLocale().getLanguage();
    log.info("Default language {}", defaultLanguage);
    log.info("Language tag {}", languageTag);
    if (!Objects.equals(defaultLanguage, languageTag)) {
      Arrays.stream(target.getClass().getDeclaredFields())
        .filter(it -> it.isAnnotationPresent(Id.class))
        .findFirst().ifPresent(idField -> {
          idField.setAccessible(true);
          try {
            String entity = target.getClass().getSimpleName();
            translateFields(target, (Integer) idField.get(target), entity + ".", languageTag);
          } catch (IllegalAccessException e) {
            log.error("Can't access the field id", e);
          }
        });
    }
  }

  private void translateFields(Object target, Integer entityId, String entity, String languageTag) {
    List<Translation> translations = translationRepository.findTranslation(entityId, entity, languageTag);

    Class<?> clazz = target.getClass();
    while (clazz != null) {
      translateFields(target, target.getClass(), translations);
      clazz = clazz.getSuperclass();
    }

  }

  private void translateFields(Object target, Class<?> clazz, List<Translation> translations) {
    Arrays.stream(clazz.getDeclaredFields())
      .filter(it -> it.isAnnotationPresent(I18n.class))
      .forEach(processI18nField(target, translations));
  }


  private Consumer<Field> processI18nField(Object target, List<Translation> translations) {
    return field -> {
      String key = field.getName();
      int entity = target.getClass().getSimpleName().length() + 1;
      translations.stream()
        .filter(it -> {
          log.info("Target: {}", key);
          log.info("Column: {}", it.getColumn().substring(entity));
          return key.equals(it.getColumn().substring(entity));
        })
        .findFirst()
        .ifPresent(translateIfPossible(target, field));
    };
  }

  private Consumer<Translation> translateIfPossible(Object target, Field field) {
    return translation -> {
      if (Strings.isNotBlank(translation.getTranslation())) {
        field.setAccessible(true);
        try {
          field.set(target, translation.getTranslation());
        } catch (IllegalAccessException e) {
          log.error("Can't set the i18n field", e);
        }
      }
    };
  }
}
