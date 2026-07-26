package org.sitmun.administration.event;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sitmun.SitmunConstants;
import org.sitmun.authorization.client.service.ProxyMiddlewareUrlResolver;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

/**
 * Protects {@code language.default} from direct mutation, and ensures {@code proxy} is always
 * persisted normalized (blank/invalid values become the Spring/config default).
 */
@Component
@RepositoryEventHandler
@RequiredArgsConstructor
public class ConfigurationParameterEventHandler {

  private static final String PROTECTION_MESSAGE =
      """
      Cannot modify 'language.default' directly. \
      Use the language default change API (/api/language-default/change) to ensure lossless migration.\
      """;

  private final ProxyMiddlewareUrlResolver proxyMiddlewareUrlResolver;

  @HandleBeforeCreate
  @HandleBeforeSave
  public void handleBeforePersist(@NotNull ConfigurationParameter param) {
    rejectLanguageDefaultMutation(param);
    sanitizeProxy(param);
  }

  @HandleBeforeDelete
  public void handleBeforeDelete(@NotNull ConfigurationParameter param) {
    rejectLanguageDefaultMutation(param);
  }

  private static void rejectLanguageDefaultMutation(ConfigurationParameter param) {
    if (SitmunConstants.LANGUAGE_DEFAULT_CONF_KEY.equals(param.getName())) {
      throw new IllegalStateException(PROTECTION_MESSAGE.stripTrailing());
    }
  }

  private void sanitizeProxy(ConfigurationParameter param) {
    if (!SitmunConstants.PROXY_CONF_KEY.equals(param.getName())) {
      return;
    }
    var result = proxyMiddlewareUrlResolver.sanitizeStoredProxy(param.getValue());
    param.setValue(result.value());
    param.setWarnings(result.infos().isEmpty() ? null : List.copyOf(result.infos()));
  }
}
