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
    if (!shouldApplyTranslation()) {
      return; // SKIP i18n when lang parameter is not present
    }
    applicationContext.getBean(TranslationService.class).updateInternationalization(target);
  }

  private boolean shouldApplyTranslation() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        String requestPath = attributes.getRequest().getRequestURI();
        String langParam = attributes.getRequest().getParameter("lang");

        // Apply translation when:
        // 1. Explicit lang parameter is present
        // e.g /api/code-list-values?lang=es&codeListName=territory.scope
        if (langParam != null && !langParam.isEmpty()) {
          return true;
        }

        // 2. Client configuration endpoints
        // e.g backend/api/config/client
        if (requestPath != null && requestPath.contains("/api/config/client")) {
          return true;
        }
      }
    } catch (Exception e) {
      log.debug("Could not determine request parameters, skipping internationalization", e);
    }
    return false;
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext)
      throws BeansException {
    this.applicationContext = applicationContext;
  }
}
