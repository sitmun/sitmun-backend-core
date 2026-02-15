package org.sitmun.infrastructure.web.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.RepositoryRestHandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Registers MVC interceptors on Spring Data REST handler mapping.
 */
@Configuration
public class RepositoryRestInterceptorConfigurer {

  private static final Logger log = LoggerFactory.getLogger(RepositoryRestInterceptorConfigurer.class);

  @Bean
  public static BeanPostProcessor repositoryRestHandlerMappingInterceptorPostProcessor(
      ListableBeanFactory beanFactory) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof RepositoryRestHandlerMapping handlerMapping) {
          RequestTraceInterceptor requestTraceInterceptor =
              beanFactory.getBean(RequestTraceInterceptor.class);
          LocaleChangeInterceptor localeChangeInterceptor =
              beanFactory.getBean(LocaleChangeInterceptor.class);
          log.info(
              "Configuring RepositoryRestHandlerMapping bean={} mappingClass={}",
              beanName,
              handlerMapping.getClass().getName());
          List<Object> currentInterceptors = readInterceptors(handlerMapping);
          int before = currentInterceptors.size();
          appendIfMissing(currentInterceptors, requestTraceInterceptor);
          appendIfMissing(currentInterceptors, localeChangeInterceptor);
          handlerMapping.setInterceptors(currentInterceptors.toArray(new Object[0]));
          log.info(
              "RepositoryRestHandlerMapping interceptors configured: before={} after={} list={}",
              before,
              currentInterceptors.size(),
              interceptorNames(currentInterceptors));
        }
        return bean;
      }

      @SuppressWarnings("unchecked")
      private List<Object> readInterceptors(RepositoryRestHandlerMapping handlerMapping) {
        try {
          Field field = AbstractHandlerMapping.class.getDeclaredField("interceptors");
          field.setAccessible(true);
          Object value = field.get(handlerMapping);
          if (value instanceof List<?> list) {
            return new ArrayList<>((List<Object>) list);
          }
        } catch (NoSuchFieldException | IllegalAccessException e) {
          log.warn("Could not read RepositoryRestHandlerMapping interceptors reflectively", e);
        }
        return new ArrayList<>();
      }

      private void appendIfMissing(List<Object> interceptors, Object candidate) {
        boolean alreadyPresent =
            interceptors.stream()
                .anyMatch(existing -> existing != null && existing.getClass().equals(candidate.getClass()));
        if (!alreadyPresent) {
          interceptors.add(candidate);
          log.info(
              "RepositoryRestHandlerMapping interceptor added: {}",
              candidate.getClass().getName());
        } else {
          log.info(
              "RepositoryRestHandlerMapping interceptor already present: {}",
              candidate.getClass().getName());
        }
      }

      private String interceptorNames(List<Object> interceptors) {
        List<String> names = new ArrayList<>();
        for (Object interceptor : interceptors) {
          names.add(interceptor == null ? "null" : interceptor.getClass().getSimpleName());
        }
        return String.join(", ", names);
      }
    };
  }
}
