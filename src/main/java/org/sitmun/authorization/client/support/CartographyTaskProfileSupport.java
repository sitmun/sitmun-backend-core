package org.sitmun.authorization.client.support;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_LAYERS;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_SERVICE;
import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.PARAMETER_WFS_TYPENAME;
import static org.sitmun.domain.DomainConstants.Services.isWfsService;

import java.util.Map;
import org.sitmun.authorization.client.dto.profile.ServiceParameter;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.territory.Territory;

/** Shared proxy URL + profile parameter slots for cartography query/edition tasks. */
public final class CartographyTaskProfileSupport {

  private CartographyTaskProfileSupport() {}

  /**
   * Builds the middleware proxy URL and adds {@code service}, {@code typename}/{@code layers}
   * parameter slots expected by viewers.
   *
   * @param parametersDto mutable profile parameters map
   * @param proxyBaseUrl middleware base URL (module-specific injection)
   * @param parameterTypeForSlots {@link org.sitmun.domain.DomainConstants.Tasks#PARAM_TYPE_QUERY}
   *     or other slot type marker for {@linkplain
   *     org.sitmun.authorization.client.AuthorizationConstants.TaskDto#PARAMETER_TYPE client
   *     profile maps}
   * @return the proxy URL string
   */
  public static String putCartographyProxyAndLayerSlots(
      Map<String, Object> parametersDto,
      String proxyBaseUrl,
      Application application,
      Territory territory,
      Cartography cartography,
      String parameterTypeForSlots) {

    Service service = cartography.getService();
    String url =
        ProxyUrlBuilder.forCartographyService(proxyBaseUrl, application, territory, service);
    parametersDto.put(
        PARAMETER_SERVICE,
        new ServiceParameter(parameterTypeForSlots, /* required */ true, service.getType()));
    String layersCsv = cartography.getLayers().stream().reduce((a, b) -> a + "," + b).orElse("");
    if (isWfsService(service)) {
      parametersDto.put(
          PARAMETER_WFS_TYPENAME,
          new ServiceParameter(parameterTypeForSlots, /* required */ true, layersCsv));
    } else {
      parametersDto.put(
          PARAMETER_LAYERS,
          new ServiceParameter(parameterTypeForSlots, /* required */ true, layersCsv));
    }
    return url;
  }
}
