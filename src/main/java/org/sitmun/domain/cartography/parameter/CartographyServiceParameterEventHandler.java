package org.sitmun.domain.cartography.parameter;

import jakarta.validation.constraints.NotNull;
import org.sitmun.authorization.proxy.parameter.ParameterScopes;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.parameter.ServiceParameterEventHandler;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class CartographyServiceParameterEventHandler {

  public static final String PROXIED_REQUIRED =
      "Layer service parameters require a proxied service. Remove them before turning proxy off.";

  @HandleBeforeCreate
  public void handleBeforeCreate(@NotNull CartographyServiceParameter parameter) {
    requireProxiedService(parameter);
  }

  @HandleBeforeSave
  public void handleBeforeSave(@NotNull CartographyServiceParameter parameter) {
    requireProxiedService(parameter);
  }

  static void requireProxiedService(CartographyServiceParameter parameter) {
    Cartography cartography = parameter.getCartography();
    Service service = cartography == null ? null : cartography.getService();
    if (service == null || !Boolean.TRUE.equals(service.getIsProxied())) {
      throw new BusinessRuleException(ProblemTypes.DATA_INTEGRITY_VIOLATION, PROXIED_REQUIRED);
    }
    if (!ParameterScopes.allowed(service.getType(), parameter.getType())) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          ServiceParameterEventHandler.TYPE_NOT_ALLOWED.formatted(
              parameter.getType(), service.getType()));
    }
  }
}
