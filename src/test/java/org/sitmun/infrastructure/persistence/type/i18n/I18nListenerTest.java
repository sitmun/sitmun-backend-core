package org.sitmun.infrastructure.persistence.type.i18n;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@DisplayName("I18nListener")
class I18nListenerTest {

  @Mock private ApplicationContext applicationContext;
  @Mock private TranslationService translationService;

  private I18nListener listener;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    listener = new I18nListener();
    listener.setApplicationContext(applicationContext);
    lenient()
        .when(applicationContext.getBean(TranslationService.class))
        .thenReturn(translationService);
    request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @AfterEach
  void resetRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
  @DisplayName(
      "mutating request with lang does not overlay translations or mark the entity read-only")
  void mutatingRequestWithLangSkipsTranslation(String method) {
    request.setMethod(method);
    request.setRequestURI("/api/trees/1");
    request.setParameter("lang", "ca");
    Object entity = new Object();

    listener.updateInternationalization(entity);

    verify(translationService, never()).updateInternationalization(entity);
  }

  @Test
  @DisplayName("GET with lang still applies the translation overlay")
  void getWithLangAppliesTranslation() {
    request.setMethod("GET");
    request.setRequestURI("/api/trees/1");
    request.setParameter("lang", "ca");
    Object entity = new Object();

    listener.updateInternationalization(entity);

    verify(translationService).updateInternationalization(entity);
  }

  @Test
  @DisplayName("client configuration GET still applies translation without a lang parameter")
  void clientConfigurationGetStillAppliesTranslation() {
    request.setMethod("GET");
    request.setRequestURI("/api/config/client");
    Object entity = new Object();

    listener.updateInternationalization(entity);

    verify(translationService).updateInternationalization(entity);
  }
}
