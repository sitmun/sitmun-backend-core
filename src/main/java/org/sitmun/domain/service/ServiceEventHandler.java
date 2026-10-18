package org.sitmun.domain.service;

import jakarta.validation.constraints.NotNull;
import org.sitmun.authorization.proxy.parameter.ParameterScopes;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameter;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameterEventHandler;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository;
import org.sitmun.domain.service.parameter.ServiceParameter;
import org.sitmun.domain.service.parameter.ServiceParameterEventHandler;
import org.sitmun.domain.service.parameter.ServiceParameterRepository;
import org.sitmun.infrastructure.persistence.exception.BusinessRuleException;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class ServiceEventHandler {

  static final String AUTH_NONE = "None";

  private final CartographyServiceParameterRepository cartographyServiceParameterRepository;
  private final ServiceParameterRepository serviceParameterRepository;

  public ServiceEventHandler(
      CartographyServiceParameterRepository cartographyServiceParameterRepository,
      ServiceParameterRepository serviceParameterRepository) {
    this.cartographyServiceParameterRepository = cartographyServiceParameterRepository;
    this.serviceParameterRepository = serviceParameterRepository;
  }

  @HandleBeforeCreate
  public void handleBeforeCreate(@NotNull Service service) {
    if (service.getPassword() != null && service.getPassword().isEmpty()) {
      service.setPassword(null);
    }
    forceProxiedWhenAuthenticated(service);
  }

  @HandleBeforeSave
  public void handleBeforeSave(@NotNull Service service) {
    keepPassword(service);
    forceProxiedWhenAuthenticated(service);
    if (!Boolean.TRUE.equals(service.getIsProxied())
        && service.getId() != null
        && cartographyServiceParameterRepository.existsByCartography_Service_Id(service.getId())) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          CartographyServiceParameterEventHandler.PROXIED_REQUIRED);
    }
    requireParameterTypes(service);
  }

  private void requireParameterTypes(Service service) {
    if (service.getId() == null) {
      return;
    }
    for (ServiceParameter row : serviceParameterRepository.findByService_Id(service.getId())) {
      rejectDisallowed(service.getType(), row.getType());
    }
    for (CartographyServiceParameter row :
        cartographyServiceParameterRepository.findByCartography_Service_Id(service.getId())) {
      rejectDisallowed(service.getType(), row.getType());
    }
  }

  private static void rejectDisallowed(String serviceType, String parameterType) {
    if (!ParameterScopes.allowed(serviceType, parameterType)) {
      throw new BusinessRuleException(
          ProblemTypes.DATA_INTEGRITY_VIOLATION,
          ServiceParameterEventHandler.TYPE_NOT_ALLOWED.formatted(parameterType, serviceType));
    }
  }

  private static void keepPassword(Service service) {
    if (service.getPassword() == null) {
      service.setPassword(service.getStoredPassword());
    } else if (service.getPassword().isEmpty()) {
      service.setPassword(null);
    }
  }

  private static void forceProxiedWhenAuthenticated(Service service) {
    String mode = service.getAuthenticationMode();
    if (mode != null && !mode.isBlank() && !AUTH_NONE.equals(mode)) {
      service.setIsProxied(true);
    }
  }
}
