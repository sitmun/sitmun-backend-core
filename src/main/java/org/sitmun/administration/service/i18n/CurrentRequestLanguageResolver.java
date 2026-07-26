package org.sitmun.administration.service.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class CurrentRequestLanguageResolver {

  private final RequestLocaleResolutionService requestLocaleResolutionService;

  public String resolve(Object handler) {
    if (!(RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes)) {
      return null;
    }
    HttpServletRequest request = attributes.getRequest();
    HttpServletResponse response = attributes.getResponse();
    return requestLocaleResolutionService.resolveLanguage(request, response, handler, null);
  }
}
