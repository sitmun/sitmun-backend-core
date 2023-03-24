package org.sitmun.infrastructure.persistence.type.i18n;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.persistence.Id;
import javax.persistence.PostLoad;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Component
public class I18nListener implements ApplicationContextAware {

  private static final Logger logger = LoggerFactory.getLogger(I18nListener.class);

  private ApplicationContext context;

  @PostLoad
  public void updateInternationalization(Object target) {
    Locale requestLocale = LocaleContextHolder.getLocale();
    if (requestLocale != Locale.getDefault()) {
      String languageTag = requestLocale.toLanguageTag();
      Arrays.stream(target.getClass().getDeclaredFields())
        .filter(it -> it.isAnnotationPresent(Id.class))
        .findFirst().ifPresent(idField -> {
          idField.setAccessible(true);
          try {
            translateFields(target, (Integer) idField.get(target), languageTag);
          } catch (IllegalAccessException e) {
            logger.error("Can't access the field id", e);
          }
        });
    }
  }

  private void translateFields(Object target, Integer entityId, String languageTag) {
    List<Translation> translations = context.getBean(TranslationRepository.class).findByEntityIdAndLocale(entityId, languageTag);

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
      translations.stream()
        .filter(it -> key.equals(it.getColumn()))
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
          logger.error("Can't set the i18n field", e);
        }
      }
    };
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
    context = applicationContext;
  }
}
