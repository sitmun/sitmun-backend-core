package org.sitmun.authorization.proxy.validator;

import static org.sitmun.domain.DomainConstants.Services.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.ogc.CartographyGrant;
import org.sitmun.authorization.proxy.ogc.OgcLayerAccess;
import org.sitmun.authorization.proxy.ogc.OgcRequestNames;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.parameter.CartographyServiceParameterRepository;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Validates user access to services through the proxy middleware.
 *
 * <p>This validator enforces comprehensive authorization for service requests:
 *
 * <h3>Service-Level Authorization</h3>
 *
 * <ul>
 *   <li>Service must exist and not be blocked
 *   <li>User must hold a role of this application in the territory
 *   <li>User must have access to at least one cartography using this service
 *   <li>Roles are {@code RoleRepository.findRolesByApplicationAndUserAndTerritory}. Accessible
 *       cartographies are then {@code cartographyRepository.findByRolesAndTerritory}
 * </ul>
 *
 * <h3>Layer-Level Authorization</h3>
 *
 * <ul>
 *   <li>A request with {@code cartographyId} may name only that cartography's layers
 *   <li>GetMap requires the full layer set. GetFeatureInfo and DescribeLayer may name a subset
 *   <li>On the service path, each name must belong to some accessible cartography. A transaction is
 *       denied. GetMap, GetFeatureInfo, GetFeature, and DescribeLayer are denied when a cartography
 *       involved has its own service parameters
 * </ul>
 *
 * <p>The authorization model follows this hierarchy:
 *
 * <pre>
 * Application.availableRoles
 *   → UserConfiguration (Territory + Role)
 *     → Role → CartographyPermission
 *       → Cartography (members)
 *         → Service + layers[]
 *         → CartographyAvailability (Territory)
 * </pre>
 *
 * @see org.sitmun.authorization.client.service.AuthorizationService
 * @see org.sitmun.domain.role.RoleRepository#findRolesByApplicationAndUserAndTerritory
 * @see org.sitmun.domain.cartography.CartographyRepository#findByRolesAndTerritory
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceResourceAccessValidator implements ResourceAccessValidator {

  // Service-based proxy types (OGC services)
  // Note: API type for tasks is handled by TaskResourceAccessValidator
  private static final Set<String> SUPPORTED_TYPES = Set.of(TYPE_WMS, TYPE_WMTS, TYPE_WFS);

  private final ServiceRepository serviceRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final TerritoryRepository territoryRepository;
  private final CartographyRepository cartographyRepository;
  private final CartographyServiceParameterRepository cartographyServiceParameterRepository;
  private final RoleRepository roleRepository;

  @Override
  public boolean supports(String type) {
    return SUPPORTED_TYPES.stream().anyMatch(t -> t.equalsIgnoreCase(type));
  }

  @Override
  public boolean validate(ConfigProxyRequestDto request, String userName) {
    log.debug(
        "Validating Service access: user={}, appId={}, terId={}, type={}, serviceId={}",
        userName,
        request.getAppId(),
        request.getTerId(),
        request.getType(),
        request.getTypeId());

    var serviceOpt = serviceRepository.findById(request.getTypeId());
    if (serviceOpt.isEmpty()) {
      log.warn("Service not found: {}", request.getTypeId());
      return false;
    }

    var service = serviceOpt.get();
    if (Boolean.TRUE.equals(service.getBlocked())) {
      log.warn(
          "Access denied: Service is blocked - serviceId={}, user={}",
          request.getTypeId(),
          userName);
      return false;
    }

    var userOpt = userRepository.findByUsername(userName);
    if (userOpt.isEmpty()) {
      log.warn("User not found: {}", userName);
      return false;
    }

    var applicationExists = applicationRepository.existsById(request.getAppId());
    if (!applicationExists) {
      log.warn("Application not found: {}", request.getAppId());
      return false;
    }

    // Check if territory exists (terId can be 0 for public access)
    if (request.getTerId() > 0) {
      var territoryExists = territoryRepository.existsById(request.getTerId());
      if (!territoryExists) {
        log.warn("Territory not found: {}", request.getTerId());
        return false;
      }
    }

    var userRoles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            userName, request.getAppId(), request.getTerId());

    if (userRoles.isEmpty()) {
      log.warn(
          "Access denied: User {} has no role of application {} in territory {} - serviceId={}",
          userName,
          request.getAppId(),
          request.getTerId(),
          request.getTypeId());
      return false;
    }

    var accessibleCartographies =
        cartographyRepository.findByRolesAndTerritory(userRoles, request.getTerId());

    var serviceCartographies =
        accessibleCartographies.stream()
            .filter(
                cartography ->
                    cartography.getService() != null
                        && cartography.getService().getId().equals(service.getId()))
            .toList();

    if (serviceCartographies.isEmpty()) {
      log.warn(
          "Access denied: User {} has no cartography permissions for service {} in territory {}",
          userName,
          request.getTypeId(),
          request.getTerId());
      return false;
    }

    List<CartographyGrant> grants = serviceCartographies.stream().map(this::grant).toList();
    OgcRequestNames.View names =
        OgcRequestNames.parse(request.getParameters(), request.getRequestBody());
    boolean allowed =
        request.getCartographyId() == null
            ? OgcLayerAccess.allowService(names, grants)
            : allowCartography(request.getCartographyId(), grants, names);
    if (!allowed) {
      log.warn(
          "Access denied: user {} service {} cartography {} operation {}",
          userName,
          request.getTypeId(),
          request.getCartographyId(),
          names.operation());
      return false;
    }

    log.debug("Service access validation passed for user: {}", userName);
    return true;
  }

  private boolean allowCartography(
      Integer cartographyId, List<CartographyGrant> grants, OgcRequestNames.View names) {
    return grants.stream()
        .filter(grant -> Objects.equals(grant.id(), cartographyId))
        .findFirst()
        .map(grant -> OgcLayerAccess.allowCartography(names, grant))
        .orElse(false);
  }

  private CartographyGrant grant(Cartography cartography) {
    Set<String> layers = new LinkedHashSet<>();
    if (cartography.getLayers() != null) {
      cartography.getLayers().stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(layer -> !layer.isEmpty())
          .forEach(layers::add);
    }
    Integer id = cartography.getId();
    boolean hasParameters =
        id != null && cartographyServiceParameterRepository.existsByCartography_Id(id);
    return new CartographyGrant(id, layers, hasParameters);
  }
}
