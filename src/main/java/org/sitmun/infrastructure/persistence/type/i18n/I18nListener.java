package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.persistence.PostLoad;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class I18nListener implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  @PostLoad
  public void updateInternationalization(Object target) {
    if (isClientConfigRequest()) {
      return;
    }
    applicationContext.getBean(TranslationService.class).updateInternationalization(target);
  }

  private boolean isClientConfigRequest() {
    try {
      ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        String requestPath = attributes.getRequest().getRequestURI();
        return requestPath != null && requestPath.startsWith("/api/config/client");
      }
    } catch (Exception e) {
      log.debug("Could not determine request path, proceeding with internationalization", e);
    }
    return false;
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext)
      throws BeansException {
    this.applicationContext = applicationContext;
  }
}
