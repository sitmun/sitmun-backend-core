package org.sitmun.domain.service.parameter;

import jakarta.validation.constraints.NotNull;
import org.sitmun.authorization.proxy.parameter.ParameterScopes;
import org.sitmun.domain.service.Service;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class ServiceParameterEventHandler {

  public static final String TYPE_NOT_ALLOWED = "Parameter type %s is not allowed on a %s service.";

  @HandleBeforeCreate
  public void handleBeforeCreate(@NotNull ServiceParameter parameter) {
    requireAllowedType(parameter);
  }

  @HandleBeforeSave
  public void handleBeforeSave(@NotNull ServiceParameter parameter) {
    requireAllowedType(parameter);
  }

  static void requireAllowedType(ServiceParameter parameter) {
    Service service = parameter.getService();
    String serviceType = service == null ? null : service.getType();
    if (!ParameterScopes.allowed(serviceType, parameter.getType())) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          TYPE_NOT_ALLOWED.formatted(parameter.getType(), serviceType));
    }
  }
}
