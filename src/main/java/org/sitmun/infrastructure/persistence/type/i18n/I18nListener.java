package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.persistence.PostLoad;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class I18nListener implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  @PostLoad
  public void updateInternationalization(Object target) {
    applicationContext.getBean(TranslationService.class).updateInternationalization(target);
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }
}
