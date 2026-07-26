package org.sitmun.administration.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.SitmunConstants;
import org.sitmun.authorization.client.service.ProxyMiddlewareUrlResolver;
import org.sitmun.authorization.client.service.ProxyMiddlewareUrlResolver.ProxySanitizeResult;
import org.sitmun.domain.configuration.ConfigurationParameter;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigurationParameterEventHandler")
class ConfigurationParameterEventHandlerTest {

  @Mock private ProxyMiddlewareUrlResolver proxyMiddlewareUrlResolver;

  @InjectMocks private ConfigurationParameterEventHandler handler;

  @Test
  @DisplayName("Prevent creation of language.default parameter")
  void preventCreationOfLanguageDefault() {
    ConfigurationParameter param =
        ConfigurationParameter.builder()
            .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
            .value("fr")
            .build();

    assertThatThrownBy(() -> handler.handleBeforePersist(param))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("language.default")
        .hasMessageContaining("language default change API");
  }

  @Test
  @DisplayName("Prevent update of language.default parameter")
  void preventUpdateOfLanguageDefault() {
    ConfigurationParameter param =
        ConfigurationParameter.builder()
            .id(1)
            .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
            .value("fr")
            .build();

    assertThatThrownBy(() -> handler.handleBeforePersist(param))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("language.default")
        .hasMessageContaining("language default change API");
  }

  @Test
  @DisplayName("Prevent deletion of language.default parameter")
  void preventDeletionOfLanguageDefault() {
    ConfigurationParameter param =
        ConfigurationParameter.builder()
            .id(1)
            .name(SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY)
            .value("en")
            .build();

    assertThatThrownBy(() -> handler.handleBeforeDelete(param))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("language.default");
  }

  @Test
  @DisplayName("Allow modification of other configuration parameters")
  void allowModificationOfOtherParameters() {
    ConfigurationParameter param =
        ConfigurationParameter.builder().id(1).name("test.parameter").value("test-value").build();

    handler.handleBeforePersist(param);
    handler.handleBeforeDelete(param);

    assertThat(param.getValue()).isEqualTo("test-value");
  }

  @Test
  @DisplayName("Sanitize blank proxy value through resolver before persist")
  void sanitizeProxyBeforePersist() {
    when(proxyMiddlewareUrlResolver.sanitizeStoredProxy(""))
        .thenReturn(
            new ProxySanitizeResult(
                "http://localhost:8080/middleware",
                List.of("entity.configurationParameter.warning.proxy-defaulted")));

    ConfigurationParameter param =
        ConfigurationParameter.builder().name(SitmunConstants.PROXY_CONF_KEY).value("").build();

    handler.handleBeforePersist(param);

    assertThat(param.getValue()).isEqualTo("http://localhost:8080/middleware");
    assertThat(param.getWarnings())
        .containsExactly("entity.configurationParameter.warning.proxy-defaulted");
    verify(proxyMiddlewareUrlResolver).sanitizeStoredProxy("");
  }
}
